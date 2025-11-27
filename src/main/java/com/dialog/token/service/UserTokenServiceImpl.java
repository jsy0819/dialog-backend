package com.dialog.token.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.dialog.token.domain.UserSocialToken;
import com.dialog.token.repository.UserSocialTokenRepository;
import com.dialog.user.domain.MeetUser;
import com.dialog.user.repository.MeetUserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserTokenServiceImpl implements UserTokenService {

    private final UserSocialTokenRepository userSocialTokenRepository;
    private final MeetUserRepository meetUserRepository;

    @Override
    public void saveGoogleTokens(Long userId, String accessToken, String refreshToken, LocalDateTime expiresAt) {
        Optional<UserSocialToken> tokenOpt = userSocialTokenRepository.findByUser_IdAndProvider(userId, "google");

        UserSocialToken token = tokenOpt.orElseGet(() -> {
            UserSocialToken newToken = new UserSocialToken();
            MeetUser user = meetUserRepository.findById(userId)
            	    .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
            newToken.setUser(user);
            newToken.setProvider("google");
            return newToken;
        });

        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);
        token.setExpiresAt(expiresAt);
        userSocialTokenRepository.save(token);
    }

    @Override
    public Optional<UserSocialToken> findByUserIdAndProvider(Long userId, String provider) {
        return userSocialTokenRepository.findByUser_IdAndProvider(userId, provider);
    }
}
