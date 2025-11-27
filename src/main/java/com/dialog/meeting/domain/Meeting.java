package com.dialog.meeting.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.dialog.meetingresult.domain.MeetingResult;
import com.dialog.participant.domain.Participant;
import com.dialog.recording.domain.Recording;
import com.dialog.transcript.domain.Transcript;
import com.dialog.user.domain.MeetUser;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meeting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
public class Meeting {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 255)
	private String title;

	@Lob
	private String description;

	@Column(name = "scheduled_at", nullable = false)
	private LocalDateTime scheduledAt;

	@Column(name = "started_at")
	private LocalDateTime startedAt;

	@Column(name = "ended_at")
	private LocalDateTime endedAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private Status status = Status.SCHEDULED;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "host_user_id", nullable = false)
	private MeetUser hostUser;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	// 사용자가 지정하는 중요도 (리스트 필터링용)
	@Column(name = "is_important", nullable = false)
	@Builder.Default
	private boolean isImportant = false;

	// 사용자가 회의 생성 시 미리 입력한 '관심 키워드' (단순 텍스트로 저장)
	// 예: "예산, 일정, 기획" -> AI 분석 결과인 Keyword 엔티티와는 용도가 다름
	@Column(name = "highlight_keywords")
	private String highlightKeywords;

	// --- 연관 관계 ---

	@OneToMany(mappedBy = "meeting", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default // 빌더 패턴 사용 시 초기화 유지
	private List<Participant> participants = new ArrayList<>();

	@OneToOne(mappedBy = "meeting", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private Recording recording;

	// Transcript는 Meeting에 직접 종속 (사실 데이터)
	@OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@Builder.Default
	private List<Transcript> transcripts = new ArrayList<>();

	// MeetingResult (결과 데이터) 분리 - 1:1 매핑
	@OneToOne(mappedBy = "meeting", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private MeetingResult meetingResult;

	// --- 비즈니스 메서드 ---

	public void updateInfo(String title, String description) {
		if (title != null)
			this.title = title;
		if (description != null)
			this.description = description;
	}

	public void startRecording() {
		this.status = Status.RECORDING;
		this.startedAt = LocalDateTime.now();
	}

	public void complete() {
		this.status = Status.COMPLETED;
		this.endedAt = LocalDateTime.now();
	}

	public void toggleImportance() {
		this.isImportant = !this.isImportant;
	}

	// 연관관계 편의 메서드
	public void setMeetingResult(MeetingResult meetingResult) {
		this.meetingResult = meetingResult;
		if (meetingResult != null && meetingResult.getMeeting() != this) {
			// MeetingResult 쪽에도 meeting을 세팅해주기 위함 (순환 호출 주의 필요하지만 Setter가 없다면 Builder나 생성자에서 처리됨)
			// MeetingResult Entity에 setMeeting이 없다면 이 부분은 로직에 따라 조정 필요
		}
	}
}