package com.dialog.calendarevent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.dialog.calendarevent.domain.CalendarEventResponse;
import com.dialog.calendarevent.domain.EventDateTimeDTO;
import com.dialog.calendarevent.domain.EventType;
import com.dialog.calendarevent.domain.GoogleEventRequestDTO;
import com.dialog.calendarevent.domain.GoogleEventResponseDTO;
import com.dialog.exception.GoogleOAuthException;

import org.springframework.http.MediaType;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
//@RequiredArgsConstructor
public class GoogleCalendarApiClient {

	private final WebClient webClient;
	private final String googleCalendarUrl;
	private static final DateTimeFormatter ISO_OFFSET_DATE_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

	public GoogleCalendarApiClient(WebClient webClient, 
								  @Value("${google.api.calendar-url}") String googleCalendarUrl) {
		this.webClient = webClient;
		this.googleCalendarUrl = googleCalendarUrl;
	}

	public List<CalendarEventResponse> getEvents(String accessToken, String calendarId, LocalDateTime timeMin,
			LocalDateTime timeMax) {

		// 1. Google API가 요구하는 ISO 8601 UTC 형식으로 변환
		// Timezone을 시스템 기본값으로 설정하고 UTC로 변환하여 API 요청 파라미터를 만듭니다.
		String timeMinStr = timeMin.atZone(ZoneId.systemDefault()).toInstant().atOffset(java.time.ZoneOffset.UTC)
				.format(DateTimeFormatter.ISO_INSTANT);
		String timeMaxStr = timeMax.atZone(ZoneId.systemDefault()).toInstant().atOffset(java.time.ZoneOffset.UTC)
				.format(DateTimeFormatter.ISO_INSTANT);

		try {
			// 2. WebClient를 사용하여 요청 구성 및 실행
			GoogleEventResponseDTO eventsContainer = webClient.get().uri(this.googleCalendarUrl, uriBuilder -> uriBuilder
					// API 요구사항에 맞는 쿼리 파라미터 추가
					.queryParam("timeMin", timeMinStr).queryParam("timeMax", timeMaxStr)
					.queryParam("singleEvents", true) // 반복 일정을 개별 이벤트로 확장
					.queryParam("orderBy", "startTime") // 시작 시간 순으로 정렬
					.build(calendarId)) // 경로 변수 {calendarId} 설정

					// 3. Authorization: Bearer [accessToken] 헤더 설정
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)

					// 4. API 호출 및 응답 처리
					.retrieve().onStatus(HttpStatusCode::isError, response -> {
						log.error("Google Calendar API 호출 실패. Status: {}", response.statusCode());
						// 오류 발생 시 사용자 정의 예외를 발생시킵니다.
						// 이 단계에서 403, 401 오류 등을 잡아서 상위 레이어로 전달해야 합니다.
						return Mono.error(
								new GoogleOAuthException("Google Calendar API 호출 중 오류 발생: " + response.statusCode()));
					})
					// 응답을 GoogleEventResponseDTO 컨테이너 객체로 파싱
					.bodyToMono(GoogleEventResponseDTO.class).block(); // 동기적으로 결과 대기

			// 5. 결과 검증 및 데이터 변환
			if (eventsContainer == null || eventsContainer.getItems() == null) {
				return Collections.emptyList();
			}

			// items 리스트를 가져와 CalendarEventDTO로 최종 변환합니다.
			// (주의: GoogleEventResponseDTO 클래스 내부에 List<GoogleEventItemDTO> items가 있다고 가정)
			return eventsContainer.getItems().stream().map(this::convertToCalendarEventDTO)
					.collect(Collectors.toList());

		} catch (Exception e) {
			log.error("Google API 통신 중 예외 발생", e);

			if (e instanceof GoogleOAuthException) {
				throw (GoogleOAuthException) e; // 이미 GoogleOAuthException이면 그대로 던짐
			}
			throw new GoogleOAuthException("Google API 통신 중 예외 발생: " + e.getMessage());
		}
	}

	public GoogleEventResponseDTO createEvent(String accessToken, String calendarId, GoogleEventRequestDTO requestDTO) {

		if (requestDTO == null) {
			throw new IllegalArgumentException("Google Calendar 이벤트를 생성하기 위한 requestDTO가 null입니다. 상위 서비스 로직을 확인하세요.");
		}

		try {
			GoogleEventResponseDTO responseBody = webClient.post() // POST 요청
					.uri(this.googleCalendarUrl, calendarId) // 캘린더 ID를 경로 변수로 설정

					// 1. 헤더 설정: 인증 토큰 및 JSON 타입 명시
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).contentType(MediaType.APPLICATION_JSON)

					// 2. 요청 본문 설정: 일정 데이터를 JSON 형태로 보냅니다.
					.bodyValue(requestDTO)

					// 3. API 호출 및 응답 처리
					.retrieve().onStatus(HttpStatusCode::isError, response -> {
						log.error("Google Calendar 이벤트 생성 실패. Status: {}", response.statusCode());
						// [ 4. 수정 ] RuntimeException -> GoogleOAuthException
						return Mono.error(
								new GoogleOAuthException("Google Calendar API 생성 중 오류 발생: " + response.statusCode()));
					}).bodyToMono(GoogleEventResponseDTO.class) // 4. 응답 JSON을 DTO로 파싱
					.block(); // 동기적으로 결과 대기

			if (responseBody == null) {
				throw new GoogleOAuthException("Google Calendar 이벤트 생성 후 응답 본문이 비어있습니다.");
			}
			return responseBody;

		} catch (Exception e) {
			log.error("Google API 통신 중 일정 생성 예외 발생", e);
			throw new GoogleOAuthException("일정 생성 API 통신 실패");
		}
	}

	private CalendarEventResponse convertToCalendarEventDTO(GoogleEventResponseDTO googleEvent) {

		EventDateTimeDTO startDateTimeDTO = (EventDateTimeDTO) googleEvent.getStart();
		EventDateTimeDTO endDateTimeDTO = (EventDateTimeDTO) googleEvent.getEnd();

		LocalDateTime start = parseDateTime(startDateTimeDTO, true); // 시작 시간 파싱

		String eventDateStr = null;
		if (googleEvent.getStart().getDate() != null) {
			// "YYYY-MM-DD" 형식의 문자열로 변환
			eventDateStr = googleEvent.getStart().getDate().toString();
		} else if (googleEvent.getStart().getDateTime() != null) {
			eventDateStr = LocalDate
					.parse(googleEvent.getStart().getDateTime().toString(), DateTimeFormatter.ISO_DATE_TIME).toString();
		}

		return CalendarEventResponse.builder()
				.id(null) // Google 이벤트는 우리 DB ID가 없음
				.userId(null)
				.title(googleEvent.getSummary())
				.eventDate(eventDateStr)
				.time(null)
				.eventType("MEETING")
				.isImportant(false)
				.sourceId(googleEvent.getId())
				.googleEventId(googleEvent.getId())
				.createdAt(null)
				.status(googleEvent.getStatus())
				.build();
	}

	private LocalDateTime parseDateTime(EventDateTimeDTO dateTimeDTO, boolean isStart) {
		if (dateTimeDTO == null) {
			return null;
		}

		// 1. dateTime 필드가 있는 경우 (일반 이벤트)
		if (dateTimeDTO.getDateTime() != null) {
			try {
				// ISO 8601 (예: 2025-10-23T10:00:00+09:00) 문자열을 파싱하여 시스템 ZoneId로 변환
				return ZonedDateTime.parse(dateTimeDTO.getDateTime(), ISO_OFFSET_DATE_TIME)
						.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
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
			return localDate.atStartOfDay();
		}
		return null;
	}

	private EventType determineEventType(EventDateTimeDTO startDateTimeDTO) {
		if (startDateTimeDTO != null && startDateTimeDTO.getDate() != null) {
			return EventType.ALL_DAY; // date 필드가 있으면 종일 일정
		}
		return EventType.NORMAL; // dateTime 필드가 있으면 일반 일정 (NORMAL로 가정)
	}

	public GoogleEventResponseDTO patchEvent(String accessToken, String calendarId, String eventId,
			GoogleEventRequestDTO eventData) {

		if (eventData == null) {
			throw new IllegalArgumentException("eventData가 null입니다.");
		}

		//final String GOOGLE_EVENT_PATCH_URL = GOOGLE_CALENDAR_URL + "/{eventId}";
		String patchUrl = this.googleCalendarUrl + "/{eventId}";

		try {
			GoogleEventResponseDTO responseBody = webClient.patch().uri(patchUrl, calendarId, eventId)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).contentType(MediaType.APPLICATION_JSON)
					.bodyValue(eventData).retrieve().onStatus(HttpStatusCode::isError, response -> {
						log.error("Google Calendar 이벤트 수정(Patch) 실패. Status: {}", response.statusCode());
						return Mono.error(new RuntimeException(
								"Google Calendar API 수정(Patch) 중 오류 발생: " + response.statusCode()));
					}).bodyToMono(GoogleEventResponseDTO.class).block(Duration.ofSeconds(5)); // 5초까지 기다렸다가 구글 서버가 응답
																								// 안주면 에러발생시킴.

			if (responseBody == null) {
				throw new RuntimeException("Google Calendar 이벤트 수정 후 응답 본문이 비어있습니다.");
			}
			return responseBody;

		} catch (Exception e) {
			log.error("Google API 통신 중 일정 수정(Patch) 예외 발생", e);
			throw new RuntimeException("일정 수정(Patch) API 통신 실패", e);
		}
	}

	public void deleteEvent(String accessToken, String calendarId, String eventId) {
		//final String GOOGLE_EVENT_DELETE_URL = GOOGLE_CALENDAR_URL + "/{eventId}";
		String deleteUrl = this.googleCalendarUrl + "/{eventId}";
		try {
			webClient.delete() // delete() 메서드 사용
					.uri(deleteUrl, calendarId, eventId) // URL 변수 매핑
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken) // 헤더 설정
					.retrieve().onStatus(HttpStatusCode::isError, response -> {
						log.error("Google Calendar 이벤트 삭제(Delete) 실패. Status: {}", response.statusCode());

						if (response.statusCode().equals(HttpStatus.UNAUTHORIZED)
								|| response.statusCode().equals(HttpStatus.FORBIDDEN)) {
							return Mono.error(
									new RuntimeException("Google API 오류(invalid_grant): " + response.statusCode()));
						}
						return Mono.error(new RuntimeException(
								"Google Calendar API 삭제(Delete) 중 오류 발생: " + response.statusCode()));
					}).bodyToMono(Void.class).block();

		} catch (Exception e) {
			log.error("Google API 통신 중 일정 삭제(Delete) 예외 발생", e);
			throw new RuntimeException(e.getMessage(), e);
		}
	}
}
