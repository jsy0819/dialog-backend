package com.dialog.token.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.dialog.CalendarEvent.Service.GoogleTokenDto;
import com.dialog.GoogleAuth.DTO.GoogleAuthDTO;
import com.dialog.token.domain.UserSocialToken;
import com.dialog.token.repository.UserSocialTokenRepository;
import com.dialog.user.domain.MeetUser;
import com.dialog.user.repository.MeetUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 로그 추가
import reactor.core.publisher.Mono;

@Service 
@RequiredArgsConstructor
@Slf4j //  로그 추가
public class SocialTokenService {

	// 1. 필수 의존성 주입 (Spring Security의 토큰 저장소)
	private final OAuth2AuthorizedClientService authorizedClientService;
	private final MeetUserRepository meetUserRepository;
	//private final RefreshTokenRepository refreshTokenRepository;
	private final UserSocialTokenRepository userSocialTokenRepository;
	private final WebClient webClient;
	
	private final GoogleAuthDTO googleAuthDTO;

	public String getToken(String userEmail, String provider) throws IllegalArgumentException {
	    
	    // 1. User Email을 통해 MeetUser 엔티티를 조회하고 ID를 얻습니다.
	    // 사용자를 찾을 수 없는 경우 IllegalArgumentException 발생
	    MeetUser meetUser = meetUserRepository.findByEmail(userEmail)
	                        .orElseThrow(() -> {
	                            log.error("MeetUser를 찾을 수 없습니다: {}", userEmail);
	                            // IllegalAccessException 대신 IllegalArgumentException 사용
	                            return new IllegalArgumentException("MeetUser를 찾을 수 없습니다: " + userEmail); 
	                        });
	    
	    Long userId = meetUser.getId(); // MeetUser의 ID(PK) 획득
	    // 2. MeetUser ID를 사용하여 DB에서 UserSocialToken을 조회합니다. 
	    Optional<UserSocialToken> socialTokenOpt = userSocialTokenRepository.findByUser_IdAndProvider(userId, provider);
	    if (socialTokenOpt.isEmpty()) {
	        log.warn("사용자 ID {} (Email: {})에 대한 {} Refresh Token이 DB에 없습니다. Google 연동이 필요합니다.", 
	                 userId, userEmail, provider);
	        // DB에 토큰이 없으므로 예외 발생 (정상적인 로직)
	        throw new IllegalArgumentException(provider + " 연동 토큰이 없습니다. 먼저 계정 연동을 완료해야 합니다.");
	    }
	    UserSocialToken socialToken = socialTokenOpt.get();
	    String refreshToken = socialToken.getRefreshToken();
	  
	    return this.refreshAccessToken(refreshToken, provider.toLowerCase(),userId, socialToken);
	}

    private String refreshAccessToken(String refreshToken, String provider, Long userId, UserSocialToken socialToken) {
    	
    	GoogleAuthDTO.ProviderConfig config = googleAuthDTO.getProvider()
                .get(provider);   	
    	
        log.info("API CALL: {} 토큰 엔드포인트로 Refresh Token 갱신 요청 시도. URI: {}", provider, config.getTokenEndpoint());
        try {
            // Google Token Endpoint 호출
            GoogleTokenDto tokenResponse = webClient.post()
                    .uri(config.getTokenEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("client_id", config.getClientId())
                            .with("client_secret", config.getClientSecret())
                            .with("refresh_token", refreshToken)
                            .with("grant_type", "refresh_token"))
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), clientResponse -> {
                        // 4xx 또는 5xx 에러 발생 시 응답 바디를 로그로 출력하고 예외를 발생
                    	return clientResponse.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.error("❌ Google Token API 에러 응답 수신. 상태: {}, 바디: {}", 
                                               clientResponse.statusCode(), body);
                                    if (body.contains("invalid_grant")) {
                                        // ⭐ 2. 무효화된 토큰 DB에서 삭제 (비동기 처리)
                                        userSocialTokenRepository.findByRefreshToken(refreshToken)
                                                .ifPresent(token -> {
                                                    log.warn("무효화된 Refresh Token 삭제 (User ID: {}): {}", userId, token.getRefreshToken().subSequence(0, 10));
                                                    userSocialTokenRepository.delete(token);
                                                });
                                    }
                                    
                                    // 이 예외는 .block() 또는 .bodyToMono()에서 WebClientResponseException으로 잡힐 수 있습니다.
                                    System.out.println("여길 탔을거야 아마도!!!!!!!!!!");
                                    return Mono.error(new RuntimeException("Google 토큰 갱신 실패. 응답: " + body));
                                });
                    })
                .bodyToMono(GoogleTokenDto.class) // bodyToMono의 인자 변경
                .block();
            log.info("API RESPONSE: {} Access Token 갱신 성공. 만료 시간: {}초", provider, tokenResponse.getExpiresIn());
            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                // 토큰을 받지 못한 경우 RuntimeException 발생
                throw new RuntimeException("Google로부터 유효한 Access Token을 받지 못했습니다.");
            }     
            String newAccessToken = tokenResponse.getAccessToken();
            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn());
            // DTO에 맞게 로깅 정보 수정            
            socialToken.setAccessToken(newAccessToken);
            socialToken.setExpiresAt(expiresAt);
            userSocialTokenRepository.save(socialToken); //DB 갱신
            log.info("Access Token 갱신 성공. 만료 시간: {}초", tokenResponse.getExpiresIn());
            return newAccessToken;

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