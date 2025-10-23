package com.dialog.meeting.domain;

import java.time.LocalDateTime;

import lombok.Getter;

/**
 * (Response) 회의 생성 응답 시 서버가 보낼 데이터를 담는 클래스
 */
@Getter
public class MeetingCreateResponseDto {

    private Long meetingId;   // 새로 생성된 회의 ID
    private String title;     // 생성된 회의 제목
    private Status status;    // 현재 상태 (예: SCHEDULED)
    private LocalDateTime scheduledAt; // 예약 시간

    /**
     * Entity 객체를 DTO로 변환하는 생성자
     */
    public MeetingCreateResponseDto(Meeting meeting) {
        this.meetingId = meeting.getId();
        this.title = meeting.getTitle();
        this.status = meeting.getStatus();
        this.scheduledAt = meeting.getScheduledAt();
    }
}