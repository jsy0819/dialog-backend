package com.dialog.meeting.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.dialog.keyword.domain.Keyword;
import com.dialog.participant.domain.Participant;
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
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
	private String title; // 회의 제목
	
	@Lob 
	private String description; // 회의 설명
	
	@Column(name = "scheduled_at", nullable = false)
	private LocalDateTime scheduledAt; // 예약된 회의 시작 시간
	
	@Column(name = "started_at")
	private LocalDateTime startedAt; // 실제 회의 시작 시간
	
	@Column(name = "ended_at")
	private LocalDateTime endedAt; // 실제 회의 종료 시간
	
	@Enumerated(EnumType.STRING) 
	@Column(nullable = false)
	    @Builder.Default 
	private Status status = Status.SCHEDULED; // 회의 상태
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "host_user_id", nullable = false) 
	private MeetUser hostUser; // 회의 주최자 (MeetUser 엔티티 참조)
	
	@Lob
	private String summary; // AI 회의 요약본
	
	@CreationTimestamp // 엔티티 생성 시 자동으로 현재 시간 저장
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt; // 레코드 생성 일시
	
	@UpdateTimestamp // 엔티티 수정 시 자동으로 현재 시간 저장
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt; // 레코드 마지막 수정 일시
	
	@OneToMany(mappedBy = "meeting", fetch = FetchType.LAZY)
	private List<Participant> participants;
	
  // 키워드 연관관계 (다대다)
	@Builder.Default
    @ManyToMany
    @JoinTable(
        name = "MeetingKeyword",
        joinColumns = @JoinColumn(name = "meeting_id"),
        inverseJoinColumns = @JoinColumn(name = "keyword_id")
    )
    private List<Keyword> keywords = new ArrayList<>();
     
    public void updateInfo(String title, String description) {
        if (title != null) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
    }
   
    //  회의 상태를 '녹음 중(RECORDING)'으로 변경하고 시작 시간을 기록합니다.     
    public void startRecording() {
        this.status = Status.RECORDING;
        this.startedAt = LocalDateTime.now(); 
    }

    
    //  회의 상태를 '완료(COMPLETED)'로 변경하고 종료 시간을 기록합니다.
    public void complete() {
        this.status = Status.COMPLETED;
        this.endedAt = LocalDateTime.now(); 
    }


    // AI 요약본을 업데이트합니다.
    public void updateSummary(String newSummary) {
        this.summary = newSummary;
    }
}