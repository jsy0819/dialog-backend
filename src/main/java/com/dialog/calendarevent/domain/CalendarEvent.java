package com.dialog.calendarevent.domain;

import java.time.LocalDate;
import java.time.LocalTime;

import com.dialog.meeting.domain.Meeting;
import com.dialog.todo.domain.Todo;

import java.time.LocalDateTime; // 필요 시 사용

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "calendar_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 2. 보호된 기본 생성자 사용
public class CalendarEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private LocalDate eventDate;

	// 시간이 있는 일정일 경우 사용
	@Column(nullable = true)
	private LocalTime eventTime;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private EventType eventType;

	// 중요 표시 (DB: TINYINT(1) / Java: boolean)
	@Column(nullable = false)
	private boolean isImportant;

	@Column(nullable = false)
	private boolean isCompleted = false;

	// 내부 Task와 연결 (내부 일정)
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "task_id")
	private Todo task;

	// 내부 Meeting과 연결 (내부 회의)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "meeting_id")
	private Meeting meeting;

	private String googleEventId;

	// 생성/수정 시간 필드 (DB 테이블에 created_at이 있으므로 필요)
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Builder
	public CalendarEvent(Long userId, String title, LocalDate eventDate, LocalTime eventTime, EventType eventType,
			boolean isImportant, Todo task, Meeting meeting, String googleEventId) { 
		this.userId = userId;
		this.title = title;
		this.eventDate = eventDate;
		this.eventTime = eventTime;
		this.eventType = eventType;
		this.isImportant = isImportant;
		this.task = task;
		this.meeting = meeting;
		this.googleEventId = googleEventId;
		this.createdAt = LocalDateTime.now();
	}

	public void updateEventDetails(String title, LocalDate eventDate, LocalTime eventTime, EventType eventType) {
		this.title = title;
		this.eventDate = eventDate;
		this.eventTime = eventTime;
		this.eventType = eventType;
	}

	public void toggleImportance() {
		this.isImportant = !this.isImportant;
	}

	public void setIsCompleted(boolean completed) {
		this.isCompleted = completed;
	}
}
