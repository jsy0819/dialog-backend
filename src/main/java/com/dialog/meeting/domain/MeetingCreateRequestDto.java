package com.dialog.meeting.domain;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * (Request) 회의 생성 요청 시 클라이언트가 보낼 데이터를 담는 클래스
 */
@Getter
@NoArgsConstructor
public class MeetingCreateRequestDto {

    private String title;           // 'meeting-title' 인풋
    private LocalDateTime scheduledAt; // 'meeting-date' 인풋
    private String description;     // 'meeting-description' 텍스트 영역
    
    private List<String> participantNames; // 'participant-name' 인풋 목록
    private List<String> keywords;         // 'keyword-input' 인풋 목록
}