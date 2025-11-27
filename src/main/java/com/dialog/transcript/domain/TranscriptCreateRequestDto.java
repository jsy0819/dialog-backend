package com.dialog.transcript.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TranscriptCreateRequestDto {
    
    private String speakerId;       // 원본 화자 ID
    private String speakerName;     // 매핑된 실제 이름
    private Integer speakerLabel;   // CLOVA speaker label
    private String text;            // 발화 내용
    private Long startTime;         // 시작 시간 (ms)
    private Long endTime;           // 종료 시간 (ms)
    private Integer sequenceOrder;  // 발화 순서
}