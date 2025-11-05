package com.dialog.calendarevent.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleTokenDto {

	// Google은 expires_in 필드를 Integer 형태로 반환할 수 있으므로,
	// Integer나 String 또는 long으로 유연하게 받기 위해 Long으로 설정하고 @JsonProperty를 사용합니다.

	@JsonProperty("access_token")
	private String accessToken;

	@JsonProperty("expires_in")
	private Long expiresIn; // Long으로 설정하여 Integer 값도 수용 가능하게 처리

	@JsonProperty("scope")
	private String scope;

	@JsonProperty("token_type")
	private String tokenType;

	@JsonProperty("refresh_token")
	private String refreshToken; // refresh_token이 응답에 포함될 경우를 대비
}