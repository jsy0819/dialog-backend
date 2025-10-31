package com.dialog.CalendarEvent.Controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

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

	@GetMapping("/calendar/events")
	public ResponseEntity<List<CalendarEventResponse>> getEvents(Principal principal,
			@RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) // 파라미터
																												// 이름 명시
			LocalDate startDate,

			@RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) // 파라미터 이름
																												// 명시
			LocalDate endDate) throws IllegalAccessException {		

		if (principal == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		String userEmail = principal.getName();

		try {
	        // 3. Service 호출 시 이메일을 전달합니다.
	        // Service 내부에서 이 이메일을 이용해 MeetUser ID를 조회하고, Google 토큰을 찾도록 해야 합니다.
	        List<CalendarEventResponse> events = calendarEventService.getEventsByDateRange(userEmail, startDate, endDate);
	        
	        return ResponseEntity.ok(events);
	        
	    } catch (IllegalAccessException e) {
	        // 토큰이 없거나 MeetUser를 찾을 수 없는 DB/데이터 오류 처리
	        System.err.println("❌ 캘린더 조회 인증/데이터 오류: " + e.getMessage());
	        return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403 Forbidden
	        
	    } catch (Exception e) {
	        System.err.println("❌ 캘린더 조회 서버 예외 발생: " + e.getMessage());
	        e.printStackTrace();
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
	    }
	}

	@PostMapping("/calendar/events")
	public ResponseEntity<GoogleEventResponseDTO> createEvent(Principal principal,
			@RequestBody @Valid CalendarCreateRequest request) {
		log.error("flag");
		try {
			
			if (request == null || request.getEventData() == null) {
				System.out.println("❌ 요청 본문(JSON)의 eventData 필드가 누락되었거나 null입니다.");
				return ResponseEntity.badRequest().build(); // 400 Bad Request 반환
			}

			String userEmail = principal.getName();
			String provider = "google";
			
			// AccessToken 가져오기 (TokenManagerService 등에서 조회 필요)
			// 예시: String accessToken = tokenManagerService.getAccessToken(userEmail,
			// provider);
			// TODO: 실제 accessToken 조회 로직으로 교체해야 함
			String accessToken = tokenManagerService.getToken(userEmail, provider);
			log.error("flag2");
			// GoogleEventRequestDTO를 그대로 전달 (DTO 변환 불필요)
			GoogleEventResponseDTO response = calendarEventService.createCalendarEvent(userEmail, // principalName
					provider, // provider
					request.getCalendarId(), // calendarId
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
