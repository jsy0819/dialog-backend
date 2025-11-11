package com.dialog.meeting.domain;

//import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MeetingFinishRequestDto {
    
    private Integer duration;  // 회의 진행 시간 (초)
    private String endTime;    // 종료 시간 (ISO format)
//    private List<TranscriptDto> transcripts;  // 대화 내용
    private Integer sentenceCount;  // 문장 수
    private Double avgConfidence;  // 평균 신뢰도
    private RecordingData recording;  // 녹음 파일 정보
    
//    @Getter
//    @Setter
//    @NoArgsConstructor
//    public static class TranscriptDto {
//        private String speaker;  // 발화자
//        private String time;     // 시간
//        private String text;     // 내용
//    }
    
    @Getter
    @Setter
    @NoArgsConstructor
    public static class RecordingData {
        private String audioFileUrl;      // 오디오 파일 URL
        private String audioFormat;       // 오디오 형식 (wav, mp3 등)
        private Long audioFileSize;       // 파일 크기 (bytes)
        private Integer durationSeconds;  // 녹음 길이 (초)
    }
}