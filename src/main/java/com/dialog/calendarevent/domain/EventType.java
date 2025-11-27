package com.dialog.calendarevent.domain;

public enum EventType {
    NORMAL, 
    ALL_DAY, 
	TASK, // 할일 (Task) - 할일 목록과 연동
	MEETING, // 회의 (Meeting) - 회의 목록과 연동
	PERSONAL // 개인 일정/할일 - 단발성 개인 기록
}