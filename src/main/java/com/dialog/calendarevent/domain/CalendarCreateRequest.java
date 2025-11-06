package com.dialog.calendarevent.domain;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CalendarCreateRequest {

	// 일정을 등록할 캘린더 ID (예: "primary" 또는 공유 캘린더 ID)
	private String calendarId;

	@NotNull(message = "eventData는 필수입니다.")
	private GoogleEventRequestDTO eventData;

	// 1. String 대신 EventType Enum을 사용하여 타입 안전성 확보
	private EventType eventType; 
	
	private boolean isImportant;
    
    // 로컬 DB 연결을 위한 ID
    private Long referenceId; // task_id 또는 meeting_id를 담을 수 있는 범용 필드
}
