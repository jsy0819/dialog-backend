package com.dialog.GoogleAuth.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.dialog.GoogleAuth.DTO.GoogleAuthDTO;
import com.dialog.security.oauth2.CustomOAuth2User;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

	private final GoogleAuthDTO googleAuthDTO;

	public String generateAuthUrl() {
		String authUrl = UriComponentsBuilder.fromUriString(googleAuthDTO.getAuthUri())
				.queryParam("client_id", googleAuthDTO.getClientId())
				.queryParam("redirect_uri", googleAuthDTO.getRedirectUri())
				.queryParam("scope", googleAuthDTO.getScope())
				.queryParam("response_type", "code")
				.queryParam("access_type", "offline")
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
			return Long.parseLong(identifier);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("사용자 식별자 형식 오류: " + identifier);
		}
	}

	// exchangeCodeAndSaveToken의 역할 (권한 획득)
	public void exchangeCodeAndSaveToken(Long userId, String code) {
		try {
			// 1. DTO의 Scope 문자열을 List<String>으로 변환
			// (예: "scope1,scope2" -> ["scope1", "scope2"])
			List<String> scopes = Arrays.asList(googleAuthDTO.getScope().split(","));			
			// 2. Google Authorization Code Flow 객체 생성
			GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(new NetHttpTransport(), // HTTP
																												// 통신 담당
					JacksonFactory.getDefaultInstance(), // JSON 파싱 담당
					googleAuthDTO.getClientId(), 
					googleAuthDTO.getClientSecret(), // 🚨 보안: Client Secret 사용
					scopes).setAccessType("offline") // Refresh Token 발급 요청
					.build();

			// 3. 인증 코드(code)를 토큰으로 교환하는 요청 실행
			GoogleTokenResponse response = flow.newTokenRequest(code)
					.setRedirectUri(googleAuthDTO.getClientId())
					.execute();

			// 4. Refresh Token 확보 및 검사
			String refreshToken = response.getRefreshToken();

			if (refreshToken == null) {
				// Refresh Token이 없는 경우 (권한이 이미 부여되었거나 설정 오류)
				throw new IOException("Refresh Token을 발급받지 못했습니다. Google 설정(access_type=offline)을 확인하세요.");
			}

		} catch (IOException e) {
			// Google API 통신 오류(네트워크, 인증 실패 등)를 RuntimeException으로 변환하여 상위 계층(Controller)에
			// 전달
			throw new RuntimeException("Google 토큰 교환 중 통신 오류 발생: " + e.getMessage(), e);
		}
	}
}