package com.dialog.token.service;

import java.time.LocalDateTime;
import java.util.Optional;

import com.dialog.token.domain.UserSocialToken;

public interface UserTokenService {
    void saveGoogleTokens(Long userId, String accessToken, String refreshToken, LocalDateTime expiresAt);
    Optional<UserSocialToken> findByUserIdAndProvider(Long userId, String provider);
}
