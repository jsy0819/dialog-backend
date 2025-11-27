package com.dialog.meeting.domain;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
public class MeetingCreateRequestDto {

    @NotBlank(message = "회의 제목은 반드시 입력해야 합니다.")
    private String title;

    @NotBlank(message = "회의 예약 시간은 반드시 입력해야 합니다.")
    private String scheduledAt;

    @NotBlank(message = "회의 설명은 반드시 입력해야 합니다.")
    private String description;

    @NotNull(message = "참가자 목록은 반드시 입력해야 합니다.")
    @Size(min = 1, message = "참가자는 한 명 이상 입력해야 합니다.")
    private List<String> participants;

    private List<String> keywords;
}