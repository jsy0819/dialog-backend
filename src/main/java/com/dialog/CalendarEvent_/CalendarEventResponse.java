package com.dialog.CalendarEvent_;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder // DTO 생성을 쉽게 하기 위해 Builder 패턴 사용
public class CalendarEventResponse {

//	private final Long id;
//	private final Long userId; 
//	private final String title;
//	private final LocalDate date;
//	private final LocalTime time;
//	private final String type; // EventType (MEETING, TASK, PERSONAL)의 문자열 표현
//	private final boolean isImportant;
//	private final String sourceId; // taskId, meetingId, googleEventId 중 하나
//    private final LocalDateTime createdAt;

	/**
	 * Entity를 Response DTO로 변환
	 */
//	public static CalendarEventResponse from(CalendarEvent entity) {
//		String sourceId = null;
//		if (entity.getEventType() == EventType.TASK && entity.getTaskId() != null) {
//			sourceId = entity.getTaskId().toString();
//		} else if (entity.getEventType() == EventType.MEETING && entity.getMeetingId() != null) {
//			sourceId = entity.getMeetingId().toString();
//		} else if (entity.getGoogleEventId() != null) {
//			sourceId = entity.getGoogleEventId();
//		}
//
//		return CalendarEventResponse.builder()
//				.id(entity.getId())
//				.userId(entity.getUserId())
//				.title(entity.getTitle())
//				.date(entity.getEventDate())
//				.time(entity.getEventTime())
//				.type(entity.getEventType().name())
//				.isImportant(entity.isImportant())
//				.sourceId(sourceId)
//                .createdAt(entity.getCreatedAt())
//				.build();
//	}
    
    private final Long id;
    private final Long userId; 
    private final String title;
    
    // 🚨 [수정 1] JS가 "eventDate"라는 이름의 "String"을 기대합니다.
    private final String eventDate; // LocalDate -> String
    
    private final LocalTime time; // (JS에서 사용 안 함)
    
    // 🚨 [수정 2] JS가 "eventType"이라는 이름을 기대합니다.
    private final String eventType; // "type" -> "eventType"
    
    private final boolean isImportant;
    private final String sourceId; 
    private final String googleEventId; // ⭐️ JS가 사용할 googleEventId도 추가
    private final LocalDateTime createdAt;

	/**
	 * Entity를 Response DTO로 변환
     * ⭐️ JS가 기대하는 형식에 맞게 수정
	 */
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

		return CalendarEventResponse.builder()
				.id(entity.getId())
				.userId(entity.getUserId())
				.title(entity.getTitle())
                
                // [수정 3] LocalDate -> "YYYY-MM-DD" String으로 변환
                .eventDate(entity.getEventDate() != null ? entity.getEventDate().toString() : null)
                
				.time(entity.getEventTime())
                
                // [수정 4] 필드명을 "eventType"으로 변경
				.eventType(entity.getEventType().name())
                
				.isImportant(entity.isImportant())
				.sourceId(sourceId)
                .googleEventId(entity.getGoogleEventId()) // ⭐️ googleEventId 매핑 추가
                .createdAt(entity.getCreatedAt())
				.build();
	}
}
