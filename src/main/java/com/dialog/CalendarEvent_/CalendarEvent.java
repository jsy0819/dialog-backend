package com.dialog.CalendarEvent_;

import java.time.LocalDate;
import java.time.LocalTime;
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
	private LocalTime eventTime; 

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private EventType eventType;

	// 중요 표시 (DB: TINYINT(1) / Java: boolean)
	@Column(nullable = false)
	private boolean isImportant; 

	// ------------------ 관계 필드 ------------------
	private Long taskId;
	private Long meetingId;
	private String googleEventId;

    // 생성/수정 시간 필드 (DB 테이블에 created_at이 있으므로 필요)
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
	// ------------------ 1. Builder 패턴을 사용한 생성자 ------------------
	@Builder
	public CalendarEvent(Long userId, String title, LocalDate eventDate, LocalTime eventTime,
			             EventType eventType, boolean isImportant, Long taskId, Long meetingId, 
                         String googleEventId) {
		this.userId = userId;
		this.title = title;
		this.eventDate = eventDate;
		this.eventTime = eventTime;
		this.eventType = eventType;
		this.isImportant = isImportant;
		this.taskId = taskId;
		this.meetingId = meetingId;
		this.googleEventId = googleEventId;
        this.createdAt = LocalDateTime.now();
	}
    
    // ------------------ 3. 비즈니스 로직 기반 업데이트 메서드 ------------------
    /**
     * 일정 제목, 날짜, 타입 등 핵심 내용을 업데이트
     */
    public void updateEventDetails(String title, LocalDate eventDate, LocalTime eventTime, EventType eventType) {
        this.title = title;
        this.eventDate = eventDate;
        this.eventTime = eventTime;
        this.eventType = eventType;
    }
    
    /**
     * 중요 표시 상태를 토글
     */
    public void toggleImportance() {
        this.isImportant = !this.isImportant;
    }
}
