package com.dialog.token.domain;

import java.time.LocalDateTime;

import com.dialog.user.domain.MeetUser;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshTokenDto {

    private String refreshToken;       // 클라이언트에 전달할 토큰 문자열
    private LocalDateTime expiresAt;   // 토큰 만료 시각 (필요 시)
    private MeetUser user;             // 토큰 소유자 (사용자) 추가

    public RefreshTokenDto(String refreshToken, LocalDateTime expiresAt, MeetUser user) {
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
        this.user = user;
    }

    public static RefreshTokenDto fromEntity(RefreshToken token) {
        return new RefreshTokenDto(token.getRefreshToken(), token.getExpiresAt(), token.getUser());
    }
}