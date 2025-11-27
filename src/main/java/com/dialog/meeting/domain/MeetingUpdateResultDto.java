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
@JsonIgnoreProperties(ignoreUnknown = true)
public class MeetingUpdateResultDto {

	private String title;
    private String purpose;
    private String agenda;
    private String summary;
    private ImportanceData importance; // 중요도 객체
    private List<ParticipantDto> participants; // 참석자 명단
    private List<KeywordDto> keywords;
    private List<ActionItemDto> actionItems;
    private List<TranscriptDto> transcripts;
    
    @Getter
    @Setter
    @NoArgsConstructor
    public static class ParticipantDto {
        private String speakerId; // 원본 ID (예: Speaker 1)
        private String name;      // 표시 이름 (예: 가나디)
    }

	@Getter
	@Setter
	@NoArgsConstructor
	public static class ImportanceData {
		private String level;
		private String reason;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	public static class KeywordDto {
		private String text;
		private String source;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	public static class ActionItemDto {
		private String task;
		private String assignee;
		private String dueDate;
		private String source;
		// 완료 여부 추가 (프론트엔드 isCompleted 반영)
		private Boolean isCompleted;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	public static class TranscriptDto {
		private Long id; // 기존 ID 유지
		private String speaker; // 식별자 ID (예: Speaker 1)
		private String speakerName; // 표시 이름 (예: 가나디)
		private String text;
		private Long startTime;
		private Long endTime;
		private Integer sequenceOrder;
		// 삭제 여부를 받기 위한 필드 추가
		private Boolean isDeleted;
	}
}