package com.dialog.token.service;

import java.util.Optional;

import com.dialog.token.domain.RefreshToken;
import com.dialog.user.domain.MeetUser;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(MeetUser user);

    Optional<RefreshToken> findByToken(String token);

    RefreshToken verifyTokenValidity(String token);

    void revokeToken(String token);

    void deleteExpiredTokens();
}