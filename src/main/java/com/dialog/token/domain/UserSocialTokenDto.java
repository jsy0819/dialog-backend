package com.dialog.token.domain;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSocialTokenDto {

	private String accessToken;
	private String refreshToken;
	private LocalDateTime expiresAt;

	public UserSocialTokenDto(String accessToken, String refreshToken, LocalDateTime expiresAt) {
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.expiresAt = expiresAt;
	}

	public static UserSocialTokenDto fromEntity(UserSocialToken entity) {
		return new UserSocialTokenDto(entity.getAccessToken(), entity.getRefreshToken(), entity.getExpiresAt());
	}

}