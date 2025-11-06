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

	private final String eventDate;

	private final LocalTime time; 
	private final String eventType;

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
				.eventType(entity.getEventType().name())
				.isImportant(entity.isImportant()).sourceId(sourceId).googleEventId(entity.getGoogleEventId())
				.createdAt(entity.getCreatedAt()).build();
	}
}
