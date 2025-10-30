package com.dialog.CalendarEvent.Service;

import com.dialog.CalendarEvent_.CalendarEventResponse;
import com.dialog.CalendarEvent_.EventDateTimeDTO;
import com.dialog.CalendarEvent_.GoogleEventRequestDTO;
import com.dialog.CalendarEvent_.GoogleEventResponseDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import com.dialog.CalendarEvent_.EventType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component // Spring Bean으로 등록
@RequiredArgsConstructor // WebClient 주입
public class GoogleCalendarApiClient {

	
	private final WebClient webClient;
	private static final String GOOGLE_CALENDAR_URL = "https://www.googleapis.com/calendar/v3/calendars/{calendarId}/events";
	private static final DateTimeFormatter ISO_OFFSET_DATE_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
	/**
	 * Google Calendar API를 호출하여 지정된 기간의 이벤트를 조회합니다.
	 */
	public List<CalendarEventResponse> getEvents(String accessToken, String calendarId, 
			LocalDateTime timeMin, LocalDateTime timeMax) {

				// 1. Google API가 요구하는 ISO 8601 UTC 형식으로 변환
				// Timezone을 시스템 기본값으로 설정하고 UTC로 변환하여 API 요청 파라미터를 만듭니다.
				String timeMinStr = timeMin.atZone(ZoneId.systemDefault()).toInstant().atOffset(java.time.ZoneOffset.UTC)
						.format(DateTimeFormatter.ISO_INSTANT);
				String timeMaxStr = timeMax.atZone(ZoneId.systemDefault()).toInstant().atOffset(java.time.ZoneOffset.UTC)
						.format(DateTimeFormatter.ISO_INSTANT);

		try {
			// 2. WebClient를 사용하여 요청 구성 및 실행
			GoogleEventResponseDTO eventsContainer = webClient.get()
					.uri(GOOGLE_CALENDAR_URL, uriBuilder -> uriBuilder
					// API 요구사항에 맞는 쿼리 파라미터 추가
					.queryParam("timeMin", timeMinStr)
					.queryParam("timeMax", timeMaxStr)
					.queryParam("singleEvents", true) // 반복 일정을 개별 이벤트로 확장
					.queryParam("orderBy", "startTime") // 시작 시간 순으로 정렬
					.build(calendarId)) // 경로 변수 {calendarId} 설정

					// 3. Authorization: Bearer [accessToken] 헤더 설정
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
					
					// 4. API 호출 및 응답 처리
					.retrieve()
					.onStatus(HttpStatusCode::isError, response -> {
						log.error("Google Calendar API 호출 실패. Status: {}", response.statusCode());
						// 오류 발생 시 사용자 정의 예외를 발생시킵니다.
						// 이 단계에서 403, 401 오류 등을 잡아서 상위 레이어로 전달해야 합니다.
						return Mono.error(
								new RuntimeException("Google Calendar API 호출 중 오류 발생: " + response.statusCode()));
					})
					// 응답을 GoogleEventResponseDTO 컨테이너 객체로 파싱
					.bodyToMono(GoogleEventResponseDTO.class)
					.block(); // 동기적으로 결과 대기

			// 5. 결과 검증 및 데이터 변환
			if (eventsContainer == null || eventsContainer.getItems() == null) {
				return Collections.emptyList();
			}

			// items 리스트를 가져와 CalendarEventDTO로 최종 변환합니다. 
			// (주의: GoogleEventResponseDTO 클래스 내부에 List<GoogleEventItemDTO> items가 있다고 가정)
			return eventsContainer.getItems().stream()
					.map(this::convertToCalendarEventDTO)
					.collect(Collectors.toList());

		} catch (Exception e) {
			log.error("Google API 통신 중 예외 발생", e);
			// 통신 실패 시 빈 리스트를 반환하여 서비스 로직이 중단되지 않도록 처리합니다.
			// 현재 담당자 부재로 인한 403 오류 상황에서도 앱이 작동하도록 도와줍니다.
			return Collections.emptyList();
		}
	}

	public GoogleEventResponseDTO createEvent(String accessToken, String calendarId, GoogleEventRequestDTO requestDTO) {

		if (requestDTO == null) {
	        throw new IllegalArgumentException("Google Calendar 이벤트를 생성하기 위한 requestDTO가 null입니다. 상위 서비스 로직을 확인하세요.");
	    }
		
		try {
			GoogleEventResponseDTO responseBody = webClient.post() // POST 요청
					.uri(GOOGLE_CALENDAR_URL, calendarId) // 캘린더 ID를 경로 변수로 설정

					// 1. 헤더 설정: 인증 토큰 및 JSON 타입 명시
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
					.contentType(MediaType.APPLICATION_JSON)

					// 2. 요청 본문 설정: 일정 데이터를 JSON 형태로 보냅니다.
					.bodyValue(requestDTO)

					// 3. API 호출 및 응답 처리
					.retrieve()
					.onStatus(HttpStatusCode::isError, response -> {
						log.error("Google Calendar 이벤트 생성 실패. Status: {}", response.statusCode());
						return Mono.error(
								new RuntimeException("Google Calendar API 생성 중 오류 발생: " + response.statusCode()));
					})
					.bodyToMono(GoogleEventResponseDTO.class) // 4. 응답 JSON을 DTO로 파싱
					.block(); // 동기적으로 결과 대기

			if (responseBody == null) {
				throw new RuntimeException("Google Calendar 이벤트 생성 후 응답 본문이 비어있습니다.");
			}
			return responseBody;

		} catch (Exception e) {
			log.error("Google API 통신 중 일정 생성 예외 발생", e);
			throw new RuntimeException("일정 생성 API 통신 실패", e);
		}
	}

	/**
	 * Google Calendar API 응답 객체(GoogleEventResponseDTO)를 내부 표준
	 * DTO(CalendarEventDTO)로 변환합니다.
	 */
	private CalendarEventResponse convertToCalendarEventDTO(GoogleEventResponseDTO googleEvent) {

		EventDateTimeDTO startDateTimeDTO = (EventDateTimeDTO) googleEvent.getStart(); 
		EventDateTimeDTO endDateTimeDTO = (EventDateTimeDTO) googleEvent.getEnd(); 

		LocalDateTime start = parseDateTime(startDateTimeDTO, true); // 시작 시간 파싱
		
		// (임시) 시간 정보는 null로 처리하고, 빌더 패턴을 사용하여 DTO를 생성합니다.
		return CalendarEventResponse.builder()
				// 1. Google Event ID -> CalendarEventDTO의 sourceId
				.sourceId(googleEvent.getId())

				// 2. Google Event Summary -> CalendarEventDTO의 title
				.title(googleEvent.getSummary())

				// 3. Google Event는 외부 이벤트이므로 로컬 DB ID는 null
				.id(null)

				// 4. 유형 (예: "GOOGLE" 또는 "PERSONAL"로 가정)
				.type("GOOGLE")

				// 5. 날짜와 시간 (Object 타입 변환의 복잡성 때문에 일단 null)
				.date(start != null ? start.toLocalDate() : null)
				// 종일 일정이면 LocalTime.MIN 대신 null을 넣는 것이 데이터 무결성에 더 좋을 수 있습니다.
				.time(start != null && startDateTimeDTO.getDateTime() != null ? start.toLocalTime() : null) 

				// 6. 중요 여부 (기본값 false로 가정)
				.isImportant(false)

				.build(); // ⭐⭐⭐ new 생성자 대신 .builder() 호출 ⭐⭐⭐
	}

	/**
	 * EventDateTimeDTO에서 실제 LocalDateTime 또는 LocalDate를 추출하여 LocalDateTime으로 변환합니다.
	 * - dateTime 필드가 있으면, 해당 필드(ISO 8601)를 파싱합니다. - date 필드가 있으면, 종일 일정으로 간주하고 해당
	 * 날짜의 00:00(시작) 또는 다음 날 00:00(종료)로 파싱합니다.
	 * 
	 * @param dateTimeDTO EventDateTimeDTO (start 또는 end)
	 * @return LocalDateTime
	 */
	private LocalDateTime parseDateTime(EventDateTimeDTO dateTimeDTO, boolean isStart) {
		if (dateTimeDTO == null) {
			return null;
		}

		// 1. dateTime 필드가 있는 경우 (일반 이벤트)
		if (dateTimeDTO.getDateTime() != null) {
			try {
				// ISO 8601 (예: 2025-10-23T10:00:00+09:00) 문자열을 파싱하여 시스템 ZoneId로 변환
				return ZonedDateTime.parse(dateTimeDTO.getDateTime(), ISO_OFFSET_DATE_TIME).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
			} catch (java.time.format.DateTimeParseException e) {
				log.error("Failed to parse dateTime: {}", dateTimeDTO.getDateTime(), e);
				return null;
			}
		}

		// 2. date 필드가 있는 경우 (종일 일정)
		else if (dateTimeDTO.getDate() != null) {
			java.time.LocalDate localDate = java.time.LocalDate.parse(dateTimeDTO.getDate());

			// 종일 일정의 'start'는 해당 날짜의 00:00:00
			if (isStart) {
				return localDate.atStartOfDay();
			} 
			// 종일 일정의 'end'는 Google이 다음 날 00:00:00으로 반환합니다.
			// 이를 '종료일의 23:59:59.999...'로 변환하지 않고, API가 준대로 다음 날 00:00:00을 반환합니다.
			// 클라이언트 측에서 렌더링 시 이 정보를 기반으로 처리합니다.
			return localDate.atStartOfDay();
		}
		return null;
	}

	/**
	 * 이벤트의 시작 시간 DTO를 기반으로 이벤트 유형(NORMAL 또는 ALL_DAY)을 결정합니다.
	 * 
	 * @param startDateTimeDTO 이벤트 시작 시간 DTO
	 * @return EventType
	 */
	private EventType determineEventType(EventDateTimeDTO startDateTimeDTO) {
		if (startDateTimeDTO != null && startDateTimeDTO.getDate() != null) {
			return EventType.ALL_DAY; // date 필드가 있으면 종일 일정
		}
		return EventType.NORMAL; // dateTime 필드가 있으면 일반 일정 (NORMAL로 가정)
	}
}
