package com.dialog.meeting.domain;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MeetingFinishRequestDto {
    
    private Integer duration;  // 회의 진행 시간 (초)
    private String endTime;    // 종료 시간 (ISO format)
    private RecordingData recording;  // 녹음 파일 정보
    private List<TranscriptData> transcripts;
    
    @Getter
    @Setter
    @NoArgsConstructor
    public static class RecordingData {
        private String audioFileUrl;      // 오디오 파일 URL
        private String audioFormat;       // 오디오 형식 (wav, mp3 등)
        private Long audioFileSize;       // 파일 크기 (bytes)
        private Integer durationSeconds;  // 녹음 길이 (초)
    }
    
    // 🆕 발화자 구분 데이터 (신뢰도 제외)
    @Getter
    @Setter
    @NoArgsConstructor
    public static class TranscriptData {
        private String speakerId;       // 원본 화자 ID (예: spk-1)
        private String speakerName;     // 매핑된 실제 이름
        private Integer speakerLabel;   // CLOVA speaker label
        private String text;            // 발화 내용
        private Long startTime;         // 시작 시간 (ms)
        private Long endTime;           // 종료 시간 (ms)
        private Integer sequenceOrder;  // 발화 순서
    }
}