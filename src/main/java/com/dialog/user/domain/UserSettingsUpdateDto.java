package com.dialog.user.domain;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * '설정' 페이지의 개인정보(직무, 직급) 수정을 위한 DTO.
 * - settings.js의 savePersonalInfo() 요청을 받습니다.
 */
@Getter
@NoArgsConstructor
public class UserSettingsUpdateDto {

	// 클라이언트(JS)에서 "BACKEND_DEVELOPER" 같은 문자열로 보내면
    // Spring이 알아서 Job Enum 타입으로 변환해줍니다.
	@NotNull(message = "직무를 선택해주세요.")
	private Job job;

	@NotNull(message = "직급을 선택해주세요.")
	private Position position;

}
