package com.dialog.recording.domain;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RecordingCreateRequestDto {

    @NotBlank(message = "오디오 파일 URL은 필수입니다.")
    private String audioFileUrl;

    private Long audioFileSize;

    private String audioFormat;

    private Integer durationSeconds;
}