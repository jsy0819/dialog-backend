package com.dialog.calendarevent.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
@Builder
@NoArgsConstructor 
@AllArgsConstructor 
public class CalendarEventResponse {

	private Long id; // final 제거 확인됨 (Good!)
	private Long userId;
	private String title;

	private String eventDate;

	private LocalTime time;
	private String eventType;
	
	@JsonProperty("isImportant")
	private boolean isImportant;
	private String sourceId;
	private String googleEventId;
	private LocalDateTime createdAt;

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
				.time(entity.getEventTime()).eventType(entity.getEventType().name()).isImportant(entity.isImportant())
				.sourceId(sourceId).googleEventId(entity.getGoogleEventId()).createdAt(entity.getCreatedAt()).build();
	}
}
