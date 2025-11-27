package com.dialog.meeting.domain;

import java.util.List;

import com.dialog.meetingresult.domain.ImportanceLevel;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AISummaryResponse {
    private boolean success;
    private AISummaryData summary; // "summary" 키 안에 객체가 들어옴
    private String error;

    @Getter
    @NoArgsConstructor
    public static class AISummaryData {
        private String purpose;
        private String agenda;
        private String overallSummary; 		// Python 변수명과 일치
        private ImportanceLevel importance;	// "HIGH", "MEDIUM", "LOW" 문자열
        private List<String> keywords;
    }
}