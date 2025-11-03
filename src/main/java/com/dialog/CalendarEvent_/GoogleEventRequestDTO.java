package com.dialog.CalendarEvent_;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * JS의 const event = { ... } 객체와 직접적으로 매핑되는 DTO. Google Calendar API의 Event 객체
 * 형식을 따릅니다.
 */

@Getter
@Setter
@ToString
public class GoogleEventRequestDTO {
	private String summary;
	private String description;

	// EventDateTimeDTO를 사용하여 start/end 객체를 받습니다.
	private EventDateTimeDTO start;
	private EventDateTimeDTO end;
	private Map<String, Object> eventData;
}
