package com.dialog.meeting.domain;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MeetingCreateResponseDto {

    private Long meetingId;   // 새로 생성된 회의 ID
    private String title;     // 생성된 회의 제목
    private Status status;    // 현재 상태 (예: SCHEDULED)
    private LocalDateTime scheduledAt; // 예약 시간
    private List<String> participants; // 참가자 이름 리스트
    private List<String> keywords;     // 키워드 리스트

    // Entity 객체를 DTO로 변환하는 생성자
    public MeetingCreateResponseDto(Meeting meeting, List<String> participants, List<String> keywords) {
        this.meetingId = meeting.getId();
        this.title = meeting.getTitle();
        this.status = meeting.getStatus();
        this.scheduledAt = meeting.getScheduledAt();
        this.participants = participants;
        this.keywords = keywords;
    }
}