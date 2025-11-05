package com.dialog.calendarevent.domain;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder // DTO 생성을 쉽게 하기 위해 Builder 패턴 사용
public class CalendarEventResponse {

	private final Long id;
	private final Long userId;
	private final String title;

	private final String eventDate; // LocalDate -> String

	private final LocalTime time; // (JS에서 사용 안 함)
	private final String eventType; // "type" -> "eventType"

	private final boolean isImportant;
	private final String sourceId;
	private final String googleEventId;
	private final LocalDateTime createdAt;

	public static CalendarEventResponse from(CalendarEvent entity) {
		if (entity == null) {
			return null;
		}

		String sourceId = null;
		if (entity.getEventType() == EventType.TASK && entity.getTaskId() != null) {
			sourceId = entity.getTaskId().toString();
		} else if (entity.getEventType() == EventType.MEETING && entity.getMeetingId() != null) {
			sourceId = entity.getMeetingId().toString();
		} else if (entity.getGoogleEventId() != null) {
			sourceId = entity.getGoogleEventId();
		}

		return CalendarEventResponse.builder().id(entity.getId()).userId(entity.getUserId()).title(entity.getTitle())
				.eventDate(entity.getEventDate() != null ? entity.getEventDate().toString() : null)

				.time(entity.getEventTime())

				// [수정 4] 필드명을 "eventType"으로 변경
				.eventType(entity.getEventType().name())

				.isImportant(entity.isImportant()).sourceId(sourceId).googleEventId(entity.getGoogleEventId()) // ⭐️
																												// googleEventId
																												// 매핑 추가
				.createdAt(entity.getCreatedAt()).build();
	}
}
