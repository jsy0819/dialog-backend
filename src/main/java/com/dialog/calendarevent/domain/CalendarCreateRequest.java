package com.dialog.calendarevent.domain;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * JS 클라이언트에서 Spring Controller로 전송하는 최종 요청 DTO. Google Calendar API 호출에 필요한
 * 데이터와 함께 추가적인 메타데이터를 담을 수 있습니다.
 */
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
