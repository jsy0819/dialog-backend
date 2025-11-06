package com.dialog.calendarevent.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dialog.calendarevent.domain.CalendarCreateRequest;
import com.dialog.calendarevent.domain.CalendarEventResponse;
import com.dialog.calendarevent.domain.GoogleEventResponseDTO;
import com.dialog.calendarevent.service.CalendarEventService;
import com.dialog.token.service.SocialTokenService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CalendarEventController {

	private final CalendarEventService calendarEventService;
	private final SocialTokenService tokenManagerService;

	@GetMapping("/calendar/events")
	public ResponseEntity<?> getEvents(Principal principal,

			@RequestParam(name = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

			@RequestParam(name = "endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

		if (principal == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		String userEmail = principal.getName();
		try {
			List<CalendarEventResponse> events = calendarEventService.getEventsByDateRange(userEmail, startDate,
					endDate);

			return ResponseEntity.ok(events);

		} catch (IllegalAccessException e) {

			return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403 Forbidden

		} catch (Exception e) {

			if (e.getMessage() != null
					&& (e.getMessage().contains("invalid_grant") || e.getMessage().contains("Google 토큰 갱신 실패")
							|| e.getMessage().contains("Access Token이 유효하지 않거나 비어있습니다"))) {

				log.warn("⚠️ Google 토큰 갱신 실패(invalid_grant). 401 상태와 재연동 코드를 반환합니다.");

				Map<String, String> errorResponse = new HashMap<>();
				errorResponse.put("errorCode", "GOOGLE_REAUTH_REQUIRED");
				errorResponse.put("message", "Google 토큰이 만료되었거나 무효화되었습니다. 계정 재연동이 필요합니다.");

				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse); // ⭐️ 401 + JSON 반환
			}

			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PostMapping("/calendar/events")
	public ResponseEntity<GoogleEventResponseDTO> createEvent(Principal principal,
			@RequestBody @Valid CalendarCreateRequest request) {
		try {

			if (request == null || request.getEventData() == null) {
				return ResponseEntity.badRequest().build(); // 400 Bad Request 반환
			}

			String userEmail = principal.getName();
			String provider = "google";

			String accessToken = tokenManagerService.getToken(userEmail, provider);

			String calendarId = request.getCalendarId();

			// 2. 만약 null이거나 비어있다면, "primary"를 기본값으로 사용합니다.
			if (calendarId == null || calendarId.isBlank()) {
				calendarId = "primary";
			}
			GoogleEventResponseDTO response = calendarEventService.createCalendarEvent(userEmail, // principalName
					provider, // provider
					calendarId,
					accessToken, // accessToken
					request.getEventData() // GoogleEventRequestDTO
			);

			return ResponseEntity.ok(response);

		} catch (IllegalArgumentException e) {
			// 토큰 획득/유효성 검증 오류 시
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build(); // 400 Bad Request

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PutMapping("/calendar/events/{id}")
	public ResponseEntity<?> updateEvent(Principal principal, @PathVariable("id") String eventId,
			@RequestBody @Valid CalendarCreateRequest request) {

		if (principal == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		try {
			String userEmail = principal.getName();
			String provider = "google";

			String calendarId = request.getCalendarId();
			if (calendarId == null || calendarId.isBlank()) {
				calendarId = "primary";
			}
			
			GoogleEventResponseDTO updatedEvent = calendarEventService.updateCalendarEvent(userEmail, provider,
					calendarId, eventId, // 업데이트할 이벤트의 ID
					request.getEventData() // 새로운 이벤트 정보
			);

			return ResponseEntity.ok(updatedEvent);

		} catch (IllegalAccessException e) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403

		} catch (Exception e) {
			if (e.getMessage() != null
					&& (e.getMessage().contains("invalid_grant") || e.getMessage().contains("Google 토큰 갱신 실패"))) {

				log.warn("⚠️ Google 토큰 갱신 실패(invalid_grant). 401 상태와 재연동 코드를 반환합니다.");
				Map<String, String> errorResponse = new HashMap<>();
				errorResponse.put("errorCode", "GOOGLE_REAUTH_REQUIRED");
				errorResponse.put("message", "Google 토큰이 만료되었거나 무효화되었습니다. 계정 재연동이 필요합니다.");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse); // ⭐️ 401 + JSON 반환
			}
			log.error("Error updating event: " + eventId, e);
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@DeleteMapping("/calendar/events/{id}")
	public ResponseEntity<?> deleteEvent(
	        Principal principal,
	        @PathVariable("id") String eventId) { // URL 경로의 이벤트 ID 받기

	    if (principal == null) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
	    }

	    try {
	        String userEmail = principal.getName();	        
	        calendarEventService.deleteCalendarEvent(userEmail, eventId);
	        return ResponseEntity.ok().build(); 

	    } catch (IllegalAccessException e) {
	        return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403 (권한 없음)
	    
	    } catch (RuntimeException e) {	        
	        if (e.getMessage() != null && e.getMessage().contains("invalid_grant")) {	            
	            Map<String, String> errorResponse = new HashMap<>();
	            errorResponse.put("errorCode", "GOOGLE_REAUTH_REQUIRED");
	            errorResponse.put("message", "Google 토큰이 만료되었거나 무효화되었습니다. 계정 재연동이 필요합니다.");
	            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
	        }	        
	        
	        if (e.getMessage() != null && e.getMessage().contains("로컬 이벤트를 찾을 수 없습니다")) {
	             return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404
	        }
	        log.error("Error deleting event: " + eventId, e);
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
	    }
	}
	// 캘린더 중요도 표시 하는 API
	@PatchMapping("/{eventId}/importance")
    public ResponseEntity<Void> toggleImportance(@PathVariable Long eventId) {
        calendarEventService.toggleImportance(eventId);
        return ResponseEntity.ok().build();
    }
}
