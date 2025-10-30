package com.dialog.CalendarEvent_;

import lombok.Getter;
import lombok.Setter;

/**
 * JS의 start: { 'date': 'YYYY-MM-DD' } 구조와 매핑되는 DTO. Google Calendar API 형식에 맞추어
 * 날짜 문자열을 받습니다.
 */
@Getter
@Setter
public class EventDateTimeDTO {
	// 종일 일정인 경우: YYYY-MM-DD 문자열을 받습니다.
	private String date;

	// 시간 정보가 포함된 일정인 경우: YYYY-MM-DDTHH:MM:SS+HH:MM 문자열을 받습니다.
	private String dateTime;
}
