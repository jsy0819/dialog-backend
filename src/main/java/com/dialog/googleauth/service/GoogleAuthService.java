package com.dialog.googleauth.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import com.dialog.exception.GoogleTokenExchangeException;
import com.dialog.exception.UserNotFoundException;
import com.dialog.googleauth.domain.GoogleAuthDTO;
import com.dialog.security.oauth2.CustomOAuth2User;
import com.dialog.token.domain.UserSocialToken;
import com.dialog.token.repository.UserSocialTokenRepository;
import com.dialog.user.domain.MeetUser;
import com.dialog.user.repository.MeetUserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthService {

    private final GoogleAuthDTO googleAuthDTO;
    private final MeetUserRepository meetUserRepository;
    private final UserSocialTokenRepository tokenRepository;

    private GoogleAuthDTO.ProviderConfig getGoogleConfig() {
        if (googleAuthDTO == null || googleAuthDTO.getProvider() == null) {
            throw new IllegalStateException("GoogleAuthDTO 설정이 로드되지 않았습니다. (YML 확인 필요)");
        }
        GoogleAuthDTO.ProviderConfig config = googleAuthDTO.getProvider().get("google");
        if (config == null) {
            throw new IllegalStateException("YML 설정 파일에 'oauth2.provider.google' 설정이 없습니다.");
        }
        return config;
    }

    public String generateAuthUrl(Long userId) { // 2. Long userId 인자 추가

        GoogleAuthDTO.ProviderConfig config = getGoogleConfig();
        String state = Base64.getUrlEncoder()
        		.withoutPadding()
        		.encodeToString(userId.toString().getBytes());

        String authUrl = UriComponentsBuilder.fromUriString(config.getAuthUri())
            .queryParam("client_id", config.getClientId())
            .queryParam("redirect_uri", config.getRedirectUri()) // 4. config 객체 사용
            .queryParam("scope", config.getScope()) // 4. config 객체 사용
            .queryParam("response_type", "code")
            .queryParam("access_type", "offline")
            .queryParam("state", state) // 5. state 파라미터 추가
            .queryParam("prompt", "consent")
            .build().toUriString();
        return authUrl;
    }

    public Long extractUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("인증 정보가 null입니다.");
        }

        if (userDetails instanceof CustomOAuth2User customOAuth2User) {
            return customOAuth2User.getMeetuser().getId();
        }
        String email = userDetails.getUsername();
        MeetUser user = meetUserRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("DB에서 사용자를 찾을 수 없습니다: " + email));
        return user.getId();
    }
    @Transactional
    public void exchangeCodeAndSaveToken(Long userId, String code) {

        GoogleAuthDTO.ProviderConfig config = getGoogleConfig();

        try {
        	GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    new NetHttpTransport(), JacksonFactory.getDefaultInstance(),
                    config.getClientId(), config.getClientSecret(),
                    Arrays.asList(config.getScope().split(" ")))
                    .setAccessType("offline")
                    .build();

            GoogleTokenResponse response = flow.newTokenRequest(code)
                    .setRedirectUri(config.getRedirectUri())
                    .setGrantType("authorization_code")
                    .execute();

            String accessToken = response.getAccessToken();
            String refreshToken = response.getRefreshToken(); // null일 수 있음
            Long expiresInSeconds = response.getExpiresInSeconds();
            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expiresInSeconds);

            MeetUser user = meetUserRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("사용자 없음 ID: " + userId));
            
            UserSocialToken token = tokenRepository.findByUser_IdAndProvider(userId, "google")
                    .orElseGet(() -> {
                        // 신규 생성
                        UserSocialToken newToken = new UserSocialToken();
                        newToken.setUser(user);
                        newToken.setProvider("google");
                        return newToken;
                    });

            if (refreshToken != null) {
                token.setRefreshToken(refreshToken);
            } else if (token.getRefreshToken() == null) {
                log.warn("Refresh Token이 발급되지 않았습니다. (userId={})", userId);
            }

            token.setAccessToken(accessToken);
            token.setExpiresAt(expiresAt);
            
            tokenRepository.save(token); // save는 insert/update 모두 처리

        } catch (IOException e) {
            log.error("구글 토큰 교환 실패", e);
            throw new GoogleTokenExchangeException("Google 토큰 통신 오류", e);
        }
    }
}