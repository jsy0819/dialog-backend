package com.dialog.keyword.domain;

import com.dialog.meetingresult.domain.MeetingResult;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "meeting_result_keyword",
    // [!!] (meeting_result_id + keyword_id) 조합이 중복되지 않도록 UQ 제약조건 추가
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_result_keyword",
            columnNames = {"meeting_result_id", "keyword_id"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingResultKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MeetingResult (1) : (M) MeetingResultKeyword
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_result_id", nullable = false)
    private MeetingResult meetingResult;

    // Keyword (1) : (M) MeetingResultKeyword
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keyword_id", nullable = false)
    private Keyword keyword;

    // [!!] (User's name) 님이 요청하신 '출처' 컬럼
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private KeywordSource source;

    // --- 생성자 ---
    @Builder
    public MeetingResultKeyword(MeetingResult meetingResult, Keyword keyword, KeywordSource source) {
        this.meetingResult = meetingResult;
        this.keyword = keyword;
        this.source = source;
    }

    // --- 연관관계 편의 메서드 ---
    public void setMeetingResult(MeetingResult meetingResult) {
        this.meetingResult = meetingResult;
    }
}