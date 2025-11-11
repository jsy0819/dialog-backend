package com.dialog.recording.domain;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RecordingResponseDto {

	private Long recordingId;
	private Long meetingId;
	private String audioFileUrl;
	private Long audioFileSize;
	private String audioFormat;
	private Integer durationSeconds;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public RecordingResponseDto(Recording recording) {
		this.recordingId = recording.getId();

		if (recording.getMeeting() != null) {
			this.meetingId = recording.getMeeting().getId();
		}

		this.audioFileUrl = recording.getAudioFileUrl();
		this.audioFileSize = recording.getAudioFileSize();
		this.audioFormat = recording.getAudioFormat();
		this.durationSeconds = recording.getDurationSeconds();
		this.createdAt = recording.getCreatedAt();
		this.updatedAt = recording.getUpdatedAt();
	}
}