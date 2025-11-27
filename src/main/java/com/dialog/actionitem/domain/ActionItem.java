package com.dialog.actionitem.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.dialog.meetingresult.domain.MeetingResult;
import com.dialog.user.domain.MeetUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "action_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
public class ActionItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 어느 회의 결과에 속하는지 (N:1)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "meeting_result_id", nullable = false)
	private MeetingResult meetingResult;

	// 담당자 (M:1) - 캘린더 연동을 위해 User와 연결
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assignee_user_id") // 담당자가 없을 수도 있음 (nullable)
	private MeetUser assignee;

	@Column(nullable = false, length = 1000)
	private String task; // 할 일 내용

	@Column(name = "is_completed", nullable = false)
	@Builder.Default
	private boolean isCompleted = false; // 완료 여부

	@Column(name = "due_date")
	private LocalDateTime dueDate; // 마감 기한 (캘린더 연동용)

	// 출처 정보 (AI 또는 USER) - 필수는 아님
	@Column(length = 20)
	private String source;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	// --- 편의 메서드 ---
	public void setMeetingResult(MeetingResult meetingResult) {
		this.meetingResult = meetingResult;
	}

}
