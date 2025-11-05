package com.dialog.calendarevent.domain;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

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
