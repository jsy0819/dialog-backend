package com.dialog.googleauth.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
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

@Service
@RequiredArgsConstructor
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

        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(userId.toString().getBytes());

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

        String identifier = userDetails.getUsername();

        // JWT의 sub 필드에 Long userId를 문자열로 담았다고 가정하고 파싱합니다.
        try {
            MeetUser user = meetUserRepository.findByEmail(identifier)
                                  .orElseThrow(() -> new UserNotFoundException("DB에서 사용자를 찾을 수 없습니다: " + identifier));

            return user.getId();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("사용자 식별자 형식 오류: " + identifier);
        }
    }

    public void exchangeCodeAndSaveToken(Long userId, String code) {

        GoogleAuthDTO.ProviderConfig config = getGoogleConfig();

        try {
            List<String> scopes = Arrays.asList(config.getScope().split(" ")); // 콤마가 아닌 공백
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(new NetHttpTransport(),
                JacksonFactory.getDefaultInstance(),
                config.getClientId(),
                config.getClientSecret(),
                scopes).setAccessType("offline")
                .build();

            GoogleTokenResponse response = flow.newTokenRequest(code)
                .setRedirectUri(config.getRedirectUri())
                .setGrantType("authorization_code") // 명시적으로 추가
                .execute();

            String refreshToken = response.getRefreshToken();
            String accessToken = response.getAccessToken();
            Long expiresInSeconds = response.getExpiresInSeconds();

            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expiresInSeconds);

            if (refreshToken == null) {
                throw new GoogleTokenExchangeException("Refresh Token을 발급받지 못했습니다. (Google 계정 설정에서 앱 권한 삭제 후 재시도 필요)");
            }

            MeetUser user = meetUserRepository.findById(userId)
                        .orElseThrow(() -> new UserNotFoundException("토큰을 저장할 사용자를 찾을 수 없습니다. ID: " + userId));

            Optional<UserSocialToken> existingTokenOpt = tokenRepository.findByUser_IdAndProvider(userId, "google");

            if (existingTokenOpt.isPresent()) {
                UserSocialToken existingToken = existingTokenOpt.get();
                existingToken.setAccessToken(accessToken);
                existingToken.setRefreshToken(refreshToken);
                existingToken.setExpiresAt(expiresAt);
                tokenRepository.save(existingToken);
            } else {
                UserSocialToken newGoogleToken = new UserSocialToken();
                newGoogleToken.setUser(user);
                newGoogleToken.setProvider("google");
                newGoogleToken.setAccessToken(accessToken);
                newGoogleToken.setRefreshToken(refreshToken);
                newGoogleToken.setExpiresAt(expiresAt);
                tokenRepository.save(newGoogleToken);
            }

        } catch (IOException e) {
            throw new GoogleTokenExchangeException("Google 토큰 교환 중 통신 오류 발생: " + e.getMessage(), e);
        }
    }
}