package com.dialog.calendarevent.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import com.dialog.exception.ResourceNotFoundException;
import com.dialog.meeting.domain.Meeting;
import com.dialog.meeting.domain.Status;
import com.dialog.meeting.repository.MeetingRepository;
import com.dialog.todo.domain.Todo;
import com.dialog.todo.repository.TodoRepository;
import com.dialog.token.service.SocialTokenService;
import com.dialog.user.domain.MeetUser;
import com.dialog.user.repository.MeetUserRepository;

import jakarta.persistence.EntityNotFoundException;
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
	private final MeetingRepository meetingRepository;
	private final TodoRepository todoRepository;

	public List<CalendarEventResponse> getEventsByDateRange(String userEmail, LocalDate startDate, LocalDate endDate) {

	    // 사용자 검증
	    MeetUser meetUser = meetUserRepository.findByEmail(userEmail)
	            .orElseThrow(() -> new ResourceNotFoundException("MeetUser를 찾을 수 없습니다: " + userEmail));

	    Long userId = meetUser.getId();
	    String accessToken = tokenManagerService.getToken(userEmail, "google");

	    // 로컬 DB 조회 (Task 및 로컬 이벤트)
	    // List는 나중에 구글 이벤트를 추가해야 하므로 수정 가능한 ArrayList로 감싸는 것이 안전합니다.
	    List<CalendarEvent> localEvents = calendarEventRepository.findByUserIdAndEventDateBetween(userId, startDate, endDate);
	    
	    List<CalendarEventResponse> responseEvents = localEvents.stream()
	            .map(CalendarEventResponse::from)
	            .collect(Collectors.toList());

	    // 토큰이 없으면 로컬 데이터만 반환
	    if (accessToken == null || accessToken.isEmpty()) {
	        log.warn("Google AccessToken이 없어 로컬 데이터만 반환합니다.");
	        return responseEvents;
	    }

	    try {
	        // 구글 캘린더 API 호출
	        List<CalendarEventResponse> googleEvents = googleCalendarApiClient.getEvents(accessToken, "primary",
	                startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
	        
	        // 로컬 DB에 이미 저장된 구글 이벤트 ID를 매핑 (중복 방지용)
	        Map<String, CalendarEventResponse> localGoogleMap = responseEvents.stream()
	                .filter(e -> e.getGoogleEventId() != null)
	                .collect(Collectors.toMap(CalendarEventResponse::getGoogleEventId, e -> e, (oldValue, newValue) -> oldValue));

	        // 구글 이벤트 병합 및 필터링
	        for (CalendarEventResponse gEvent : googleEvents) {
	            
	            // 이미 로컬 DB에 존재하는 이벤트는 건너뜀 (로컬 데이터 우선)
	            if (localGoogleMap.containsKey(gEvent.getGoogleEventId())) {
	                continue;
	            }

	            // 삭제된(cancelled) 이벤트 필터링 (좀비 데이터 방지 핵심)
	            // DTO에 'private String status;' 필드가 있어야 동작합니다.
	            if (gEvent.getStatus() != null && "cancelled".equalsIgnoreCase(gEvent.getStatus())) {
	                continue;
	            }
	            
	            // 검증 통과된 구글 전용 일정만 리스트에 추가
	            responseEvents.add(gEvent); 
	        }
	        
	        return responseEvents;

	    } catch (Exception e) {
	        String errorMessage = (e.getMessage() != null) ? e.getMessage() : "";

	        // 토큰 만료 관련 에러 처리
	        if (errorMessage.contains("invalid_grant") || errorMessage.contains("토큰 갱신 실패") || errorMessage.contains("401")) {
	            throw new GoogleOAuthException("Google 토큰이 만료되었거나 무효화되었습니다. 재연동이 필요합니다.");
	        }
	        
	        // 그 외 API 오류는 로그를 남기고 런타임 예외 발생
	        log.error("Google Calendar API 조회 중 심각한 오류 발생", e);
	        throw new RuntimeException("Google Calendar API 조회 중 오류가 발생했습니다.", e);
	    }
	}

	@Transactional
	public GoogleEventResponseDTO createCalendarEvent(String principalName, String provider, String calendarId,
			String accessToken, GoogleEventRequestDTO eventData) {

		GoogleEventResponseDTO responseFromGoogle = googleCalendarApiClient.createEvent(accessToken, calendarId,
				eventData);

		try {
			MeetUser user = meetUserRepository.findByEmail(principalName)
					.orElseThrow(() -> new IllegalArgumentException("DB 저장 실패: 사용자를 찾을 수 없습니다: " + principalName));

			LocalDate eventDate;
			LocalTime eventTime = null;
			EventType type = EventType.TASK; // 기본값 TASK

			// 제목 처리 (null 방지)
			String title = eventData.getSummary();
			if (title == null || title.trim().isEmpty()) {
				title = "(제목 없음)";
			}

			if (eventData.getStart().getDate() != null) {
				// Case A: 종일 일정 (TASK)
				eventDate = LocalDate.parse(eventData.getStart().getDate());
				type = EventType.TASK;
			} else if (eventData.getStart().getDateTime() != null) {
				// Case B: 시간 일정 (MEETING)
				String dateTimeStr = eventData.getStart().getDateTime();
				ZonedDateTime zdt;
				try {
					zdt = ZonedDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);
				} catch (Exception e) {
					LocalDateTime ldt = LocalDateTime.parse(dateTimeStr);
					zdt = ldt.atZone(java.time.ZoneId.systemDefault());
				}
				eventDate = zdt.toLocalDate();
				eventTime = zdt.toLocalTime();
				type = EventType.MEETING;
			} else {
				eventDate = LocalDate.now();
			}

			Todo savedTodo = null;
			if (type == EventType.TASK) {
				Todo newTodo = Todo.builder().title(title).description(eventData.getDescription()) // 구글 설명 -> Todo 설명
						.dueDate(eventDate) // 날짜 -> 마감일
						.user(user).build();

				savedTodo = todoRepository.save(newTodo); // Todo 저장
				log.info("연관 Todo 생성 완료: ID={}", savedTodo.getId());
			}

			// CalendarEvent 생성 (Todo 객체 연결)
			CalendarEvent newLocalEvent = CalendarEvent.builder().userId(user.getId()).title(title).eventDate(eventDate)
					.eventTime(eventTime).googleEventId(responseFromGoogle.getId()).eventType(type).isImportant(false)
					.task(savedTodo)
					.build();

			calendarEventRepository.save(newLocalEvent);
			log.info("로컬 DB(CalendarEvent) 저장 성공 (Google ID: {})", responseFromGoogle.getId());

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("로컬 DB 저장 실패", e);
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
			localEvent.updateEventDetails(eventData.getSummary(), LocalDate.parse(eventData.getStart().getDate()),
					localEvent.getEventTime(), localEvent.getEventType());
		} catch (Exception e) {
			log.error("로컬 DB 동기화 오류", e);
		}
		return responseFromGoogle;
	}

	@Transactional
	public void deleteCalendarEvent(String userEmail, String eventId) {
	    String accessToken = tokenManagerService.getToken(userEmail, "google");

	    MeetUser user = meetUserRepository.findByEmail(userEmail)
	            .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userEmail));

	    // 로컬 이벤트 조회
	    CalendarEvent localEvent = null;
	    Optional<CalendarEvent> byGoogleId = calendarEventRepository.findByGoogleEventIdAndUserId(eventId, user.getId());

	    if (byGoogleId.isPresent()) {
	        localEvent = byGoogleId.get();
	    } else {
	        try {
	            Long dbId = Long.parseLong(eventId);
	            localEvent = calendarEventRepository.findById(dbId)
	                    .filter(e -> e.getUserId().equals(user.getId()))
	                    .orElseThrow(() -> new ResourceNotFoundException("삭제할 이벤트를 찾을 수 없습니다. ID: " + eventId));
	        } catch (NumberFormatException nfe) {
	            throw new ResourceNotFoundException("유효하지 않은 이벤트 ID입니다: " + eventId);
	        }
	    }

	    // 구글 API 삭제 요청
	    if (localEvent.getGoogleEventId() != null && accessToken != null) {
	        try {
	            googleCalendarApiClient.deleteEvent(accessToken, "primary", localEvent.getGoogleEventId());
	        } catch (Exception e) {
	            // 에러 메시지 분석
	            String errorMsg = (e.getMessage() != null) ? e.getMessage() : "";
	            
	            // 로그에서 확인된 "404 NOT_FOUND" 메시지를 체크합니다.
	            if (errorMsg.contains("404") || errorMsg.contains("NOT_FOUND")) {
	                log.info("Google 캘린더에 이미 없는 이벤트입니다(404). 로컬 삭제를 진행합니다.");
	            } 
	            else if (errorMsg.contains("invalid_grant")) {
	                throw new GoogleOAuthException("Google 토큰이 만료되었습니다.");
	            } 
	            else {
	                // 404가 아닌 다른 에러(500 등)는 로그만 남기고 로컬 삭제 진행 (좀비 방지)
	                log.warn("Google API 삭제 요청 중 오류 발생 (로컬 삭제는 진행): {}", errorMsg);
	            }
	        }
	    }

	    // 로컬 DB 삭제 
	    calendarEventRepository.delete(localEvent);
	    log.info("로컬 이벤트 삭제 완료: {}", eventId);
	}

	@Transactional
	public void toggleImportance(String userEmail, String eventId) {
		MeetUser user = meetUserRepository.findByEmail(userEmail)
				.orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userEmail));

		Long dbId = null;
		try {
			dbId = Long.parseLong(eventId);
		} catch (NumberFormatException e) {
			dbId = null;
		}

		if (dbId == null) {
			Optional<CalendarEvent> localEventOpt = calendarEventRepository.findByGoogleEventIdAndUserId(eventId,
					user.getId());

			if (localEventOpt.isPresent()) {
				localEventOpt.get().toggleImportance();
				calendarEventRepository.save(localEventOpt.get());
			} else {
				log.info("로컬에 없는 이벤트(Google) 발견. Event ID: {}", eventId);
				String accessToken = tokenManagerService.getToken(userEmail, "google");
				if (accessToken == null) {
					throw new GoogleOAuthException("구글 연동 토큰이 만료되어 정보를 가져올 수 없습니다.");
				}

				try {
				} catch (Exception e) {
					log.error("구글 이벤트 조회 실패", e);
				}
				CalendarEvent newLocalEvent = CalendarEvent.builder().userId(user.getId()).googleEventId(eventId)
						.isImportant(true) // 새로 등록하며 '중요'로 설정
						.title("외부 일정 (동기화됨)").eventDate(LocalDate.now()).eventType(EventType.MEETING).build();
				calendarEventRepository.save(newLocalEvent);
			}

		} else {
			Optional<CalendarEvent> taskOpt = calendarEventRepository.findById(dbId);
			if (taskOpt.isPresent() && taskOpt.get().getUserId().equals(user.getId())) {
				CalendarEvent task = taskOpt.get();
				task.toggleImportance();
				calendarEventRepository.save(task);
				return;
			}
			Optional<Meeting> meetingOpt = meetingRepository.findById(dbId);
			if (meetingOpt.isPresent() && meetingOpt.get().getHostUser().getId().equals(user.getId())) {
				Meeting meeting = meetingOpt.get();
				meeting.toggleImportance(); // isImportant 필드 토글
				meetingRepository.save(meeting);
				return;
			}
			throw new ResourceNotFoundException("해당 ID(" + dbId + ")에 일치하는 이벤트(Task) 또는 회의(Meeting)가 없습니다.");
		}
	}

	@Transactional
	public void updateCompletionStatus(String userEmail, String eventId, boolean isCompleted) {
		Long dbEventId;		
		try {
			dbEventId = Long.parseLong(eventId);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("잘못된 이벤트 ID 형식입니다: " + eventId);
		}

		// 2. 사용자 정보를 조회합니다.
		MeetUser user = meetUserRepository.findByEmail(userEmail)
				.orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userEmail));

		CalendarEvent event = calendarEventRepository.findById(dbEventId)
				.orElseThrow(() -> new EntityNotFoundException("이벤트를 찾을 수 없습니다: " + dbEventId));

		if (!event.getUserId().equals(user.getId())) {
			throw new SecurityException("해당 이벤트에 접근할 권한이 없습니다.");
		}
		event.setIsCompleted(isCompleted);
		calendarEventRepository.save(event);
		log.info("이벤트 완료 상태 변경 (ID: {}, 완료: {})", eventId, isCompleted);
	}
}