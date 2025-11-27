package com.dialog.transcript.domain;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TranscriptResponseDto {
    
    private Long id;
    private Long meetingId;
    private String speakerId;
    private String speakerName;
    private Integer speakerLabel;
    private String text;
    private Long startTime;
    private Long endTime;
    private Integer sequenceOrder;
    private String timeLabel;  // 추가: 프론트 표시용
    
    @JsonProperty("isDeleted") 
    private boolean isDeleted;  // 추가: 삭제 여부
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public TranscriptResponseDto(Transcript transcript) {
        this.id = transcript.getId();
        
        if (transcript.getMeeting() != null) {
            this.meetingId = transcript.getMeeting().getId();
        }
        
        this.speakerId = transcript.getSpeakerId();
        this.speakerName = transcript.getSpeakerName();
        this.speakerLabel = transcript.getSpeakerLabel();
        this.text = transcript.getText();
        this.startTime = transcript.getStartTime();
        this.endTime = transcript.getEndTime();
        this.sequenceOrder = transcript.getSequenceOrder();
        this.timeLabel = transcript.getTimeLabel();  // 메서드로 생성
        this.isDeleted = transcript.isDeleted();
        this.createdAt = transcript.getCreatedAt();
        this.updatedAt = transcript.getUpdatedAt();
    }
}