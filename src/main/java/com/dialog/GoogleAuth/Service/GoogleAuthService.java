package com.dialog.GoogleAuth.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.dialog.GoogleAuth.DTO.GoogleAuthDTO;
import com.dialog.security.oauth2.CustomOAuth2User;
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
	/**
	 * [수정됨] 카카오/서비스 계정 연동을 위해 'state' 파라미터에 userId를 추가합니다.
	 * @param userId 현재 로그인된 사용자(예: 카카오 사용자)의 DB ID
	 */
	public String generateAuthUrl(Long userId) { // 2. Long userId 인자 추가
		
		GoogleAuthDTO.ProviderConfig config = getGoogleConfig();
		
		// 3. state 값 생성 (CSRF 방지용 임의 문자열 + Base64 인코딩된 userId)
		//    (보안 강화를 위해 CSRF 토큰을 결합하는 것이 좋으나, 우선 userId만 인코딩합니다.)
		String state = Base64.getUrlEncoder().withoutPadding().encodeToString(userId.toString().getBytes());

		String authUrl = UriComponentsBuilder.fromUriString(config.getAuthUri())
				.queryParam("client_id", config.getClientId())
				.queryParam("redirect_uri", config.getRedirectUri()) // 4. config 객체 사용
				.queryParam("scope", config.getScope()) // 4. config 객체 사용
				.queryParam("response_type", "code")
				.queryParam("access_type", "offline")
				.queryParam("state", state) // 5. state 파라미터 추가
				.build().toUriString();
		return authUrl;
	}

	public Long extractUserId(UserDetails userDetails) {
		if (userDetails == null) {
			throw new IllegalArgumentException("인증 정보가 null입니다.");
		}

		// 1. 소셜 로그인 (OAuth2)으로 인증된 경우: CustomOAuth2User 확인
		// CustomOAuth2User는 MeetUser 엔티티를 가지고 있으므로 바로 userId에 접근 가능
		if (userDetails instanceof CustomOAuth2User customOAuth2User) {
			// MeetUser user = customOAuth2User.getUser();
			return customOAuth2User.getMeetuser().getId();
		}

		// 2. 자체 로그인 (JWT)으로 인증된 경우:
		// Username(identifier)을 사용하여 ID를 추출합니다.
		String identifier = userDetails.getUsername();

		// JWT의 sub 필드에 Long userId를 문자열로 담았다고 가정하고 파싱합니다.
		try {
			MeetUser user = meetUserRepository.findByEmail(identifier)
                    .orElseThrow(() -> new IllegalArgumentException("DB에서 사용자를 찾을 수 없습니다: " + identifier));
            
            return user.getId();
			//return Long.parseLong(identifier);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("사용자 식별자 형식 오류: " + identifier);
		}
	}

public void exchangeCodeAndSaveToken(Long userId, String code) {
		
		GoogleAuthDTO.ProviderConfig config = getGoogleConfig(); // 6. config 객체 사용

		try {
			List<String> scopes = Arrays.asList(config.getScope().split(","));			
			GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(new NetHttpTransport(),
					JacksonFactory.getDefaultInstance(), 
					config.getClientId(), 
					config.getClientSecret(), // 7. config 객체 사용
					scopes).setAccessType("offline") 
					.build();

			GoogleTokenResponse response = flow.newTokenRequest(code)
					// 🚨 [치명적 오류 수정] ClientId()가 아닌 RedirectUri()를 사용해야 합니다.
					.setRedirectUri(config.getRedirectUri()) // 8. config 객체 및 redirectUri 사용
					.execute();

			String refreshToken = response.getRefreshToken();
			String accessToken = response.getAccessToken();
			Long expiresInSeconds = response.getExpiresInSeconds();

			if (refreshToken == null) {
				throw new IOException("Refresh Token을 발급받지 못했습니다.");
			}
			MeetUser user = meetUserRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("토큰을 저장할 사용자를 찾을 수 없습니다. ID: " + userId));
			LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expiresInSeconds);
			user.updateSocialToken("google", accessToken, refreshToken, expiresAt);
			// (TODO) 9. DB에 Refresh Token 저장 로직 연결
			meetUserRepository.save(user);
			// tokenRepository.saveRefreshToken(userId, refreshToken, "google");

		} catch (IOException e) {
			throw new RuntimeException("Google 토큰 교환 중 통신 오류 발생: " + e.getMessage(), e);
		}
	}
}