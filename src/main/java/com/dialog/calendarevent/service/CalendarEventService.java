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

	public List<CalendarEventResponse> getEventsByDateRange(String userEmail, LocalDate startDate, LocalDate endDate)
			throws IllegalAccessException {

		MeetUser meetUser = meetUserRepository.findByEmail(userEmail).orElseThrow(() -> {
			return new IllegalAccessException("MeetUser를 찾을 수 없습니다: " + userEmail);
		});

		Long userId = meetUser.getId();

		String principalName = userEmail;

		// 2. Google Access Token 확보 (자동 갱신 포함)
		String accessToken = tokenManagerService.getToken(principalName, "google");

		if (accessToken == null || accessToken.isEmpty()) {
			log.error("Google Access Token이 유효하지 않거나 비어있습니다. Google 캘린더 이벤트 조회를 건너뜁니다.");
			// 토큰이 없으므로 Google 이벤트는 빈 리스트 반환
			return calendarEventRepository.findByUserIdAndEventDateBetween(userId, startDate, endDate).stream()
					.map(CalendarEventResponse::from).collect(Collectors.toList());
		}

		// 3. Google Calendar API 조회
		List<CalendarEventResponse> googleEvents = googleCalendarApiClient.getEvents(accessToken, "primary",
				startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

		List<CalendarEvent> localEvents = calendarEventRepository.findByUserIdAndEventDateBetween(userId, startDate,
				endDate); // userId 변수 사용

		// 5. 결과 통합
		// 로컬 이벤트를 DTO로 변환하여 리스트 초기화
		List<CalendarEventResponse> allEvents = localEvents.stream().map(CalendarEventResponse::from) // DTO 변환 메서드 (가정)
				.collect(Collectors.toList());

		// Google 이벤트를 추가
		allEvents.addAll(googleEvents);

		return allEvents;
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
			String eventId, GoogleEventRequestDTO eventData) throws IllegalAccessException {

		String accessToken = tokenManagerService.getToken(userEmail, provider);
		if (accessToken == null || accessToken.isEmpty()) {
			log.error("Google Access Token이 유효하지 않거나 비어있습니다. Google 캘린더 이벤트 업데이트를 건너뜁니다.");
			throw new IllegalAccessException("Google Access Token이 유효하지 않거나 비어있습니다.");
		}

		GoogleEventResponseDTO responseFromGoogle = googleCalendarApiClient.patchEvent(accessToken, calendarId, eventId,
				eventData);

		try {

			MeetUser user = meetUserRepository.findByEmail(userEmail)
					.orElseThrow(() -> new IllegalArgumentException("DB 업데이트 실패: 사용자를 찾을 수 없습니다: " + userEmail));

			CalendarEvent localEvent = calendarEventRepository.findByGoogleEventIdAndUserId(eventId, user.getId())
					.orElseThrow(() -> new RuntimeException(
							"DB 업데이트 실패: Google Event ID에 해당하는 로컬 이벤트를 찾을 수 없습니다: " + eventId));

			String newTitle = eventData.getSummary();
			LocalDate newDate = LocalDate.parse(eventData.getStart().getDate());

			LocalTime existingTime = localEvent.getEventTime();
			EventType existingType = localEvent.getEventType();

			localEvent.updateEventDetails(newTitle, newDate, existingTime, existingType);

		} catch (Exception e) {
			log.error("Google 일정 수정(ID: {})은 성공했으나, 로컬 DB 동기화 중 오류 발생", eventId, e);
		}
		return responseFromGoogle;
	}

	@Transactional 
	public void deleteCalendarEvent(String userEmail, String eventId) throws IllegalAccessException {
	    String accessToken = tokenManagerService.getToken(userEmail, "google");
	    if (accessToken == null || accessToken.isEmpty()) {
	        log.error("Google Access Token이 유효하지 않습니다. Google 캘린더 이벤트 삭제를 건너뜁니다.");
	        throw new IllegalAccessException("Google Access Token이 유효하지 않거나 비어있습니다.");
	    }

	    // 2. 사용자 ID 찾기
	    MeetUser user = meetUserRepository.findByEmail(userEmail)
	            .orElseThrow(() -> new IllegalArgumentException("DB 삭제 실패: 사용자를 찾을 수 없습니다: " + userEmail));

	    CalendarEvent localEvent = calendarEventRepository.findByGoogleEventIdAndUserId(eventId, user.getId())
	            .orElseThrow(() -> new RuntimeException("DB 삭제 실패: Google Event ID에 해당하는 로컬 이벤트를 찾을 수 없습니다: " + eventId));

	    try {
	        googleCalendarApiClient.deleteEvent(accessToken, "primary", eventId);
	    } catch (Exception e) {
	        if (e.getMessage() != null && e.getMessage().contains("invalid_grant")) {
	             throw new RuntimeException("Google 토큰 갱신 실패(invalid_grant)", e);
	        }	        
	        log.warn("Google API 이벤트 삭제 중 오류 발생 (로컬 DB는 계속 삭제 시도): " + e.getMessage());
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