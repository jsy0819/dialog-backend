package com.dialog.calendarevent.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dialog.calendarevent.domain.CalendarEvent;
import com.dialog.calendarevent.domain.CalendarEventResponse;
import com.dialog.calendarevent.domain.EventType;
import com.dialog.calendarevent.domain.GoogleEventRequestDTO;
import com.dialog.calendarevent.domain.GoogleEventResponseDTO;
import com.dialog.calendarevent.repository.CalendarEventRepository;
import com.dialog.exception.GoogleOAuthException;
import com.dialog.exception.GoogleOAuthException.ResourceNotFoundException;
import com.dialog.token.service.SocialTokenService;
import com.dialog.user.domain.MeetUser;
import com.dialog.user.repository.MeetUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor // final 필드를 위한 생성자 주입
@Transactional(readOnly = true)
@Slf4j
public class CalendarEventService {

	private final CalendarEventRepository calendarEventRepository;
	private final SocialTokenService tokenManagerService;
	private final GoogleCalendarApiClient googleCalendarApiClient;
	private final MeetUserRepository meetUserRepository;

public List<CalendarEventResponse> getEventsByDateRange(String userEmail, LocalDate startDate, LocalDate endDate) {
        
        // 1. IllegalAccessException 대신 ResourceNotFoundException을 throw
        MeetUser meetUser = meetUserRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("MeetUser를 찾을 수 없습니다: " + userEmail));

        Long userId = meetUser.getId();
        String accessToken = tokenManagerService.getToken(userEmail, "google");

        if (accessToken == null || accessToken.isEmpty()) {
            log.error("Google Access Token이 유효하지 않거나 비어있습니다. 로컬 이벤트만 조회합니다.");
            return calendarEventRepository.findByUserIdAndEventDateBetween(userId, startDate, endDate).stream()
                    .map(CalendarEventResponse::from).collect(Collectors.toList());
        }

        try {
            // 3. Google Calendar API 조회
            List<CalendarEventResponse> googleEvents = googleCalendarApiClient.getEvents(accessToken, "primary",
                    startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

            List<CalendarEvent> localEvents = calendarEventRepository.findByUserIdAndEventDateBetween(userId, startDate, endDate);

            // 5. 결과 통합
            List<CalendarEventResponse> allEvents = localEvents.stream().map(CalendarEventResponse::from)
                    .collect(Collectors.toList());
            allEvents.addAll(googleEvents);

            return allEvents;

        } catch (Exception e) {           
        	String errorMessage = (e.getMessage() != null) ? e.getMessage() : "";

            // [수정됨] 401 에러도 GoogleOAuthException으로 처리하도록 조건 추가
            if (errorMessage.contains("invalid_grant") || 
                errorMessage.contains("토큰 갱신 실패") || 
                errorMessage.contains("401")) {
                throw new GoogleOAuthException("Google 토큰이 만료되었거나 무효화되었습니다. 재연동이 필요합니다.");
            }

            // 8. 그 외 모든 구글 API 관련 오류
            log.error("Google Calendar API 조회 중 심각한 오류 발생", e);
            throw new RuntimeException("Google Calendar API 조회 중 오류가 발생했습니다.", e);
        }
    }

	@Transactional // DB 쓰기를 위해 (readOnly = true) 덮어쓰기
	public GoogleEventResponseDTO createCalendarEvent(String principalName, String provider, String calendarId,
			String accessToken, GoogleEventRequestDTO eventData) {

		// 1. Google Calendar API를 호출하여 일정 생성 요청
		GoogleEventResponseDTO responseFromGoogle = googleCalendarApiClient.createEvent(accessToken, calendarId,
				eventData);

		try {
			MeetUser user = meetUserRepository.findByEmail(principalName)
					.orElseThrow(() -> new IllegalArgumentException("DB 저장 실패: 사용자를 찾을 수 없습니다: " + principalName));

			// 3. 엔티티의 @Builder를 사용하여 객체 생성
			CalendarEvent newLocalEvent = CalendarEvent.builder().userId(user.getId()).title(eventData.getSummary()) // 프론트에서
																														// 받은
																														// 제목
					.eventDate(LocalDate.parse(eventData.getStart().getDate())) // DTO의 date(String)를 LocalDate로 변환
					.googleEventId(responseFromGoogle.getId()).eventType(EventType.TASK).isImportant(false).build();

			// 4. 로컬 DB에 최종 저장
			calendarEventRepository.save(newLocalEvent);

		} catch (Exception e) {
			throw new RuntimeException("로컬 DB 저장 중 오류 발생", e);
		}
		return responseFromGoogle;
	}

	@Transactional
	public GoogleEventResponseDTO updateCalendarEvent(String userEmail, String provider, String calendarId,
            String eventId, GoogleEventRequestDTO eventData) {

        String accessToken = tokenManagerService.getToken(userEmail, provider);
        if (accessToken == null || accessToken.isEmpty()) {
            // 2. 커스텀 예외로 변경
            throw new GoogleOAuthException("Google Access Token이 유효하지 않거나 비어있습니다.");
        }

        GoogleEventResponseDTO responseFromGoogle;
        try {
            responseFromGoogle = googleCalendarApiClient.patchEvent(accessToken, calendarId, eventId, eventData);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("invalid_grant")) {
                throw new GoogleOAuthException("Google 토큰이 만료되었습니다. 재연동이 필요합니다.");
            }
            throw new RuntimeException("Google 캘린더 업데이트 실패", e);
        }

        // 3. 로컬 DB 업데이트
        MeetUser user = meetUserRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userEmail));

        CalendarEvent localEvent = calendarEventRepository.findByGoogleEventIdAndUserId(eventId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("로컬 이벤트를 찾을 수 없습니다: " + eventId));

        try {
             localEvent.updateEventDetails(eventData.getSummary(), LocalDate.parse(eventData.getStart().getDate()), localEvent.getEventTime(), localEvent.getEventType());
        } catch (Exception e) {
            log.error("로컬 DB 동기화 오류", e);
        }
        return responseFromGoogle;
    }

	@Transactional 
	public void deleteCalendarEvent(String userEmail, String eventId) {
        String accessToken = tokenManagerService.getToken(userEmail, "google");
        if (accessToken == null || accessToken.isEmpty()) {
            throw new GoogleOAuthException("Google Access Token이 유효하지 않습니다.");
        }

        MeetUser user = meetUserRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userEmail));

        CalendarEvent localEvent = calendarEventRepository.findByGoogleEventIdAndUserId(eventId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("로컬 이벤트를 찾을 수 없습니다: " + eventId));

        try {
            googleCalendarApiClient.deleteEvent(accessToken, "primary", eventId);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("invalid_grant")) {
                throw new GoogleOAuthException("Google 토큰 갱신 실패(invalid_grant)");
            }
            log.warn("Google API 삭제 실패 (로컬만 삭제 진행): " + e.getMessage());
        }
        calendarEventRepository.delete(localEvent);
    }
	// 중요도 표기 API
	public void toggleImportance(Long eventId) {
		CalendarEvent event = calendarEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("해당 일정을 찾을 수 없습니다. id=" + eventId));
        
        // 엔티티의 편의 메서드 호출 (isImportant 상태 반전)
        event.toggleImportance();		
	}
}