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

	private Long id;
	private Long userId;
	private String title;

	private String eventDate;

	private LocalTime time;
	private String eventType;
	
	@JsonProperty("isImportant")
	private boolean isImportant;
	@JsonProperty("isCompleted")
    private boolean isCompleted;
	
	private String sourceId;
	private String googleEventId;
	private LocalDateTime createdAt;
	
	private String status;

	public static CalendarEventResponse from(CalendarEvent entity) {
		if (entity == null) {
			return null;
		}

		String sourceId = null;
	    // 객체 자체가 아니라, 객체의 getId()를 호출해야 합니다.
	    if (entity.getEventType() == EventType.TASK && entity.getTask() != null) {
	        sourceId = String.valueOf(entity.getTask().getId()); 
	    } else if (entity.getEventType() == EventType.MEETING && entity.getMeeting() != null) {
	        sourceId = String.valueOf(entity.getMeeting().getId());
	    } else if (entity.getGoogleEventId() != null) {
	        sourceId = entity.getGoogleEventId();
	    }

	    return CalendarEventResponse.builder().id(entity.getId()).userId(entity.getUserId()).title(entity.getTitle())
				.eventDate(entity.getEventDate() != null ? entity.getEventDate().toString() : null)
				.time(entity.getEventTime()).eventType(entity.getEventType().name()).isImportant(entity.isImportant())
				.isCompleted(entity.isCompleted()).sourceId(sourceId).googleEventId(entity.getGoogleEventId()).createdAt(entity.getCreatedAt()).build();
	}
}
