package com.dialog.CalendarEvent.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dialog.CalendarEvent.Repository.CalendarEventRepository;
import com.dialog.CalendarEvent_.CalendarEvent;
import com.dialog.CalendarEvent_.CalendarEventResponse;
import com.dialog.CalendarEvent_.EventType;
import com.dialog.CalendarEvent_.GoogleEventRequestDTO;
import com.dialog.CalendarEvent_.GoogleEventResponseDTO;
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

	// 1. 필수 의존성 필드 선언 (Controller의 호출 로직에 맞춰 모두 주입)
	private final CalendarEventRepository calendarEventRepository;
	private final SocialTokenService tokenManagerService;
	private final GoogleCalendarApiClient googleCalendarApiClient;
	private final MeetUserRepository meetUserRepository;

	// =========================================================================
	// 1. 일정 조회 로직 (GET /api/CalendarEvents)
	// =========================================================================

	public List<CalendarEventResponse> getEventsByDateRange(String userEmail, LocalDate startDate, LocalDate endDate) throws IllegalAccessException {

	    // 1. User Email을 통해 MeetUser 엔티티를 조회하고 ID를 얻습니다. (DB 조회)
	    //    이전에 TokenManagerService에서 사용했던 로직과 동일하거나 유사해야 합니다.
	    MeetUser meetUser = meetUserRepository.findByEmail(userEmail)
	                            .orElseThrow(() -> {
	                                // 사용자를 찾을 수 없으면 IllegalAccessException 대신 IllegalArgumentException 등을 고려할 수 있지만,
	                                // 현재 Controller가 IllegalAccessException을 catch하고 있으므로, 일관성을 위해 유지합니다.
	                                return new IllegalAccessException("MeetUser를 찾을 수 없습니다: " + userEmail); 
	                            });

	    Long userId = meetUser.getId(); // ✅ 이제 userId 변수가 선언되었습니다.

	    String principalName = userEmail; // 이미 userEmail이 principalName과 동일하므로 불필요하지만, 기존 코드를 따라 유지합니다.
	    
	    // 2. Google Access Token 확보 (자동 갱신 포함)
	    String accessToken = tokenManagerService.getToken(principalName, "google");
	    
	    if (accessToken == null || accessToken.isEmpty()) {
            log.error("Google Access Token이 유효하지 않거나 비어있습니다. Google 캘린더 이벤트 조회를 건너뜁니다.");
            // 토큰이 없으므로 Google 이벤트는 빈 리스트 반환
            return calendarEventRepository.findByUserIdAndEventDateBetween(userId, startDate, endDate)
                    .stream().map(CalendarEventResponse::from).collect(Collectors.toList());
        }
	    
	    // 3. Google Calendar API 조회
	    List<CalendarEventResponse> googleEvents = googleCalendarApiClient.getEvents(accessToken, "primary",
	            startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

	    // 4. 로컬 DB 조회 (수정: userId 변수 사용)
	    // DB에서 사용자 ID와 기간을 기준으로 엔티티 목록을 조회합니다.
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

	// =========================================================================
	// 2. 일정 생성 로직 (POST /api/CalendarEvents)
	// =========================================================================

	@Transactional // DB 쓰기를 위해 (readOnly = true) 덮어쓰기
	public GoogleEventResponseDTO createCalendarEvent(String principalName, String provider, String calendarId,
			String accessToken, GoogleEventRequestDTO eventData) { 
		
		// 1. Google Calendar API를 호출하여 일정 생성 요청
		GoogleEventResponseDTO responseFromGoogle = googleCalendarApiClient.createEvent(accessToken, calendarId, eventData);

        try {
            MeetUser user = meetUserRepository.findByEmail(principalName)
                    .orElseThrow(() -> new IllegalArgumentException("DB 저장 실패: 사용자를 찾을 수 없습니다: " + principalName));
            
            // 3. 엔티티의 @Builder를 사용하여 객체 생성
            CalendarEvent newLocalEvent = CalendarEvent.builder()
                    .userId(user.getId()) 
                    .title(eventData.getSummary()) // 프론트에서 받은 제목
                    .eventDate(LocalDate.parse(eventData.getStart().getDate())) // DTO의 date(String)를 LocalDate로 변환
                    .googleEventId(responseFromGoogle.getId())
                    .eventType(EventType.TASK)
                    .isImportant(false)                     
                    .build();
            
            // 4. 로컬 DB에 최종 저장
            calendarEventRepository.save(newLocalEvent);
            
            
        } catch (Exception e) {            
            throw new RuntimeException("로컬 DB 저장 중 오류 발생", e);
        }
		return responseFromGoogle;
	}
}