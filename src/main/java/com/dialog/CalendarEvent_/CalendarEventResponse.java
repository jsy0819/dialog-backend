package com.dialog.CalendarEvent_;

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
	private final LocalDate date;
	private final LocalTime time;
	private final String type; // EventType (MEETING, TASK, PERSONAL)의 문자열 표현
	private final boolean isImportant;
	private final String sourceId; // taskId, meetingId, googleEventId 중 하나
    private final LocalDateTime createdAt;

	/**
	 * Entity를 Response DTO로 변환
	 */
	public static CalendarEventResponse from(CalendarEvent entity) {
		String sourceId = null;
		if (entity.getEventType() == EventType.TASK && entity.getTaskId() != null) {
			sourceId = entity.getTaskId().toString();
		} else if (entity.getEventType() == EventType.MEETING && entity.getMeetingId() != null) {
			sourceId = entity.getMeetingId().toString();
		} else if (entity.getGoogleEventId() != null) {
			sourceId = entity.getGoogleEventId();
		}

		return CalendarEventResponse.builder()
				.id(entity.getId())
				.userId(entity.getUserId())
				.title(entity.getTitle())
				.date(entity.getEventDate())
				.time(entity.getEventTime())
				.type(entity.getEventType().name())
				.isImportant(entity.isImportant())
				.sourceId(sourceId)
                .createdAt(entity.getCreatedAt())
				.build();
	}
}
