package com.dialog.CalendarEvent.Controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.dialog.CalendarEvent.Service.CalendarEventService;
import com.dialog.CalendarEvent_.CalendarCreateRequest;
import com.dialog.CalendarEvent_.CalendarEventResponse;
import com.dialog.CalendarEvent_.GoogleEventResponseDTO;
import com.dialog.token.service.SocialTokenService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequestMapping("/api")
// HTML 주소를 넣어야함. 반드시.
// [CORS 수정] allowCredentials를 "true"로 명시하여 쿠키 전송 허용
//@CrossOrigin(origins = { "http://localhost:5500", "http://127.0.0.1:5500" }, allowCredentials = "true") 
@RequiredArgsConstructor
public class CalendarEventController {

	private final CalendarEventService calendarEventService;
	private final SocialTokenService tokenManagerService;

//	@GetMapping("/calendar/events")
//	// public ResponseEntity<List<CalendarEventResponse>>
//	public ResponseEntity<?> getEvents(Principal principal,
//
//			@RequestParam(name = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
//
//			@RequestParam(name = "endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
//
//		if (principal == null) {
//			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//		}
//
//		String userEmail = principal.getName();
//		try {
//			List<CalendarEventResponse> events = calendarEventService.getEventsByDateRange(userEmail, startDate,
//					endDate);
//
//			return ResponseEntity.ok(events);
//
//		} catch (IllegalAccessException e) {
//
//			return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403 Forbidden
//
//		} catch (Exception e) {
//
//			// 5. 'invalid_grant' 또는 토큰 관련 예외인지 확인
//			if (e.getMessage() != null
//					&& (e.getMessage().contains("invalid_grant") || e.getMessage().contains("Google 토큰 갱신 실패")
//							|| e.getMessage().contains("Access Token이 유효하지 않거나 비어있습니다"))) {
//
//				log.warn("⚠️ Google 토큰 갱신 실패(invalid_grant). 401 상태와 재연동 코드를 반환합니다.");
//
//				// 6. 프론트엔드(JS)가 기대하는 401 + JSON 에러 응답 생성
//				Map<String, String> errorResponse = new HashMap<>();
//				errorResponse.put("errorCode", "GOOGLE_REAUTH_REQUIRED");
//				errorResponse.put("message", "Google 토큰이 만료되었거나 무효화되었습니다. 계정 재연동이 필요합니다.");
//
//				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse); // ⭐️ 401 + JSON 반환
//			}
//
//			e.printStackTrace();
//			// 그 외 모든 예외는 500
//			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//		}
//	}
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

		} 
        // ⬇️ 1. [이 블록 추가] SocialTokenService가 던진 "토큰 없음" 예외 잡기
        catch (IllegalArgumentException e) {
            log.warn("⚠️ Google 연동 토큰 없음 (IllegalArgumentException). 401 상태와 재연동 코드를 반환합니다. User: {}", userEmail);

            // ⭐️ 프론트엔드가 인지할 수 있도록 401 에러와 JSON 응답을 보냅니다.
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("errorCode", "GOOGLE_REAUTH_REQUIRED");
            errorResponse.put("message", "Google 계정 연동이 필요합니다: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
        // ⬇️ 2. [기존 블록] 접근 권한 (이건 그대로 둡니다)
        catch (IllegalAccessException e) {

			return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403 Forbidden

		} 
        // ⬇️ 3. [기존 블록] 그 외 모든 예외 (invalid_grant 등)
        catch (Exception e) {

			// "invalid_grant" (토큰 만료)는 여기에서 처리
			if (e.getMessage() != null
					&& (e.getMessage().contains("invalid_grant") || e.getMessage().contains("Google 토큰 갱신 실패")
							|| e.getMessage().contains("Access Token이 유효하지 않거나 비어있습니다"))) {

				log.warn("⚠️ Google 토큰 갱신 실패(invalid_grant). 401 상태와 재연동 코드를 반환합니다.");

				Map<String, String> errorResponse = new HashMap<>();
				errorResponse.put("errorCode", "GOOGLE_REAUTH_REQUIRED");
				errorResponse.put("message", "Google 토큰이 만료되었거나 무효화되었습니다. 계정 재연동이 필요합니다.");

				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse); // ⭐️ 401 + JSON 반환
			}
            
            // 진짜 알 수 없는 500 에러
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
					// request.getCalendarId(), // calendarId
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
}
