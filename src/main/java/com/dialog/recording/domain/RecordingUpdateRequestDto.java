package com.dialog.recording.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RecordingUpdateRequestDto {

    private String audioFileUrl;
    private Long audioFileSize;
    private String audioFormat;
    private Integer durationSeconds;
}