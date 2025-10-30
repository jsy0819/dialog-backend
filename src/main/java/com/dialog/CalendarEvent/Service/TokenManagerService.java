package com.dialog.CalendarEvent.Service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import com.dialog.token.domain.UserSocialToken;
import com.dialog.token.repository.UserSocialTokenRepository;
import com.dialog.user.domain.MeetUser;
import com.dialog.user.repository.MeetUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 로그 추가

@Service //  Spring Bean으로 등록하여 다른 클래스에서 주입받을 수 있게 함
@RequiredArgsConstructor // 생성자 주입을 위한 Lombok 어노테이션
@Slf4j //  로그 추가
public class TokenManagerService {

	// 1. 필수 의존성 주입 (Spring Security의 토큰 저장소)
	private final OAuth2AuthorizedClientService authorizedClientService;
	private final MeetUserRepository meetUserRepository;
	//private final RefreshTokenRepository refreshTokenRepository;
	private final UserSocialTokenRepository userSocialTokenRepository;
	private final WebClient webClient;
	private final Environment environment; 
    // Google OAuth2 설정 값 (application.yml 등에서 가져와야 합니다.)
    // 실제 설정 키에 맞게 수정 필요
	@Value("${spring.security.oauth2.client.registration.google.client-id}")
	private String clientId;
	@Value("${spring.security.oauth2.client.registration.google.client-secret}")
	private String clientSecret;
	
	public String getToken(String userEmail, String provider) throws IllegalArgumentException {
	    
	    // 1. User Email을 통해 MeetUser 엔티티를 조회하고 ID를 얻습니다.
	    // 사용자를 찾을 수 없는 경우 IllegalArgumentException 발생
	    MeetUser meetUser = meetUserRepository.findByEmail(userEmail)
	                        .orElseThrow(() -> {
	                            log.error("MeetUser를 찾을 수 없습니다: {}", userEmail);
	                            // 💡 IllegalAccessException 대신 IllegalArgumentException 사용
	                            return new IllegalArgumentException("MeetUser를 찾을 수 없습니다: " + userEmail); 
	                        });
	    
	    Long userId = meetUser.getId(); // ✅ MeetUser의 ID(PK) 획득

	    // 2. MeetUser ID를 사용하여 DB에서 UserSocialToken을 조회합니다. 
	    Optional<UserSocialToken> socialTokenOpt = userSocialTokenRepository.findByUser_IdAndProvider(userId, provider);
	    
	    if (socialTokenOpt.isEmpty()) {
	        // 토큰 엔티티가 없는 경우 명확한 예외 발생
	        log.warn("사용자 ID {} (Email: {})에 대한 {} Refresh Token이 DB에 없습니다. Google 연동이 필요합니다.", 
	                 userId, userEmail, provider);
	        // IllegalAccessException 대신 IllegalArgumentException 사용
	        throw new IllegalArgumentException(provider + " 연동 토큰이 없습니다. 먼저 계정 연동을 완료해야 합니다.");
	    }

	    String googleRefreshToken = socialTokenOpt.get().getRefreshToken();
	        
	    // 3. Google OAuth 서버에 Refresh Token을 사용하여 Access Token 갱신 요청
	    // 이 메서드 내부에서 RuntimeException(API 통신 오류 등)이 발생할 수 있습니다.
	    return this.refreshAccessToken(googleRefreshToken);
	}

    private String refreshAccessToken(String refreshToken) {
        log.info("Google Access Token 갱신 요청 시작");      
        try {
            // Google Token Endpoint 호출
            GoogleTokenDto tokenResponse = webClient.post() // 💡 응답 클래스 변경
                    .uri("https://oauth2.googleapis.com/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("client_id", clientId)
                            .with("client_secret", clientSecret)
                            .with("refresh_token", refreshToken)
                            .with("grant_type", "refresh_token"))
                    .retrieve()
                .bodyToMono(GoogleTokenDto.class) // 💡 bodyToMono의 인자 변경
                .block();
            
            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                // 토큰을 받지 못한 경우 RuntimeException 발생
                throw new RuntimeException("Google로부터 유효한 Access Token을 받지 못했습니다.");
            }     
            // DTO에 맞게 로깅 정보 수정
            log.info("Google Access Token 갱신 성공. 만료 시간: {}초", tokenResponse.getExpiresIn());
            return tokenResponse.getAccessToken();

        } catch (RuntimeException e) {
            // RuntimeException (API 호출 실패, JSON 파싱 실패 등)은 그대로 던짐
            throw e;
        } catch (Exception e) {
            // 그 외 모든 오류 처리 (네트워크 등)
            log.error("Google Access Token 갱신 중 일반 오류 발생", e);
            throw new RuntimeException("Google Access Token 갱신 중 통신/처리 오류 발생: " + e.getMessage());
        }
    }
}