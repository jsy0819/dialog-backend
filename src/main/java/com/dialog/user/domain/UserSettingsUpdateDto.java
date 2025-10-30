package com.dialog.user.domain;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
public class UserSettingsUpdateDto {

	@NotNull(message = "직무를 선택해주세요.")
	private Job job;

	@NotNull(message = "직급을 선택해주세요.")
	private Position position;

}