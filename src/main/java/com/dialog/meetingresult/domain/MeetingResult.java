package com.dialog.meetingresult.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.dialog.actionitem.domain.ActionItem;
import com.dialog.keyword.domain.MeetingResultKeyword;
import com.dialog.meeting.domain.Meeting;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meeting_result")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
public class MeetingResult {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Meeting과는 1:1 관계
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "meeting_id", nullable = false, unique = true)
	private Meeting meeting;

	@Lob
	private String summary; // 전체 요약

	@Lob
	private String agenda; // 주요 안건

	@Lob
	private String purpose; // 회의 목적

	// AI 분석 중요도 등급
	@Enumerated(EnumType.STRING)
	@Column(name = "importance_level")
	private ImportanceLevel importance;
	
	// 중요도 사유 컬럼
	@Column(name = "importance_reason", columnDefinition = "TEXT")
	private String importanceReason;

	// ActionItem은 결과에 종속됨 (Result 삭제 시 함께 삭제)
	@OneToMany(mappedBy = "meetingResult", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<ActionItem> actionItems = new ArrayList<>();

	// MeetingResultKeyword(연결+출처)
	@OneToMany(mappedBy = "meetingResult", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MeetingResultKeyword> keywords = new ArrayList<>();

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	// --- 편의 메서드 ---
	public void updateSummaryInfo(String purpose, String agenda, String summary, ImportanceLevel importance, String importanceReason) {
	    this.purpose = purpose;
	    this.agenda = agenda;
	    this.summary = summary;
	    this.importance = importance;
	    this.importanceReason = importanceReason;
	}

	public void setMeeting(Meeting meeting) {
		this.meeting = meeting;
	}
}