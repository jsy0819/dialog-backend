package com.dialog.meeting.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true) // 프론트에서 불필요한 필드가 와도 에러 안 나게 처리
public class MeetingUpdateResultDto {

	private String title;
	private String purpose;
	private String agenda;
	private String summary;

	// 프론트엔드 JS 객체 구조: { level: "HIGH", reason: "..." }
	private ImportanceData importance;
	private List<KeywordDto> keywords;
	private List<ActionItemDto> actionItems;
	private List<TranscriptDto> transcripts;

	@Getter
	@Setter
	@NoArgsConstructor
	public static class ImportanceData {
		private String level; // HIGH, MEDIUM, LOW
		private String reason;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	public static class ActionItemDto {
		private String task;
		private String assignee;	// 이름(String)으로 들어옴 -> DB조회 필요
		private String dueDate;		// "2025-10-15" (String) -> LocalDateTime 변환 필요
		private String source;		// "USER" or "AI"
	}
	
	// 키워드 정보를 받을 내부 DTO
    @Getter @Setter @NoArgsConstructor
    public static class KeywordDto {
        private String text;   // 키워드 내용 (예: "백엔드")
        private String source; // 출처 (예: "AI", "USER")
    }
    
    // 발화 로그 내부 DTO
    @Getter @Setter @NoArgsConstructor
    public static class TranscriptDto {
        private String speaker;     // speakerId (화자1, spk-1)
        private String speakerName; // 실제 이름 (김철수)
        private String time;        // "00:01:30"
        private String text;
        private Long startTime;
        private Long endTime;
        private Integer sequenceOrder;
    }
}