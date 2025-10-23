package com.dialog.meeting.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.dialog.user.domain.MeetUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회의(Meeting) 정보를 담는 엔티티 (DB 테이블과 1:1 매핑)
 */
@Entity
@Table(name = "meeting")
@Getter // Getter만 열어두어 데이터 조회만 허용
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA가 사용하는 기본 생성자 (외부 접근 금지)
@AllArgsConstructor // 빌더가 모든 필드를 초기화하는 생성자를 사용하도록 함
@Builder(toBuilder = true) // 빌더 패턴 활성화
public class Meeting {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id; // 회의 고유 식별자 (PK)

	@Column(nullable = false, length = 255)
	private String title; // 회의 제목

	@Lob // 긴 텍스트 (TEXT 또는 LONGTEXT)
	private String description; // 회의 설명

	@Column(name = "scheduled_at", nullable = false)
	private LocalDateTime scheduledAt; // 예약된 회의 시작 시간

	@Column(name = "started_at")
	private LocalDateTime startedAt; // 실제 회의 시작 시간

	@Column(name = "ended_at")
	private LocalDateTime endedAt; // 실제 회의 종료 시간

	@Enumerated(EnumType.STRING) // Enum 이름을 문자열로 DB에 저장
	@Column(nullable = false)
    @Builder.Default // 빌더로 객체 생성 시, 값을 안 주면 이 기본값이 설정됨
	private Status status = Status.SCHEDULED; // 회의 상태

	@ManyToOne(fetch = FetchType.LAZY) // N:1 연관관계 (지연 로딩)
	@JoinColumn(name = "host_user_id", nullable = false) // 외래키(FK)
	private MeetUser hostUser; // 회의 주최자 (MeetUser 엔티티 참조)

	@Lob
	private String summary; // AI 회의 요약본

	@CreationTimestamp // 엔티티 생성 시 자동으로 현재 시간 저장
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt; // 레코드 생성 일시

	@UpdateTimestamp // 엔티티 수정 시 자동으로 현재 시간 저장
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt; // 레코드 마지막 수정 일시

    // --- 비즈니스 로직 (Setter 대신 사용) ---

    /**
     * 회의 제목과 설명을 수정합니다. (Setter 대용)
     */
    public void updateInfo(String title, String description) {
        if (title != null) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
    }

    /**
     * 회의 상태를 '녹음 중(RECORDING)'으로 변경하고 시작 시간을 기록합니다.
     */
    public void startRecording() {
        this.status = Status.RECORDING;
        this.startedAt = LocalDateTime.now(); 
    }

    /**
     * 회의 상태를 '완료(COMPLETED)'로 변경하고 종료 시간을 기록합니다.
     */
    public void complete() {
        this.status = Status.COMPLETED;
        this.endedAt = LocalDateTime.now(); 
    }

    /**
     * AI 요약본을 업데이트합니다.
     */
    public void updateSummary(String newSummary) {
        this.summary = newSummary;
    }
}