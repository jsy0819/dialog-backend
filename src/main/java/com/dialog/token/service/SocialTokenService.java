package com.dialog.token.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import com.dialog.exception.ResourceNotFoundException;
import com.dialog.exception.GoogleOAuthException;
import com.dialog.calendarevent.service.GoogleTokenDto;
import com.dialog.googleauth.domain.GoogleAuthDTO;
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

	private final MeetUserRepository meetUserRepository;
	private final UserSocialTokenRepository userSocialTokenRepository;
	private final WebClient webClient;
	
	private final GoogleAuthDTO googleAuthDTO;

	public String getToken(String userEmail, String provider){
	    
	    // 1. User Email을 통해 MeetUser 엔티티를 조회하고 ID를 얻습니다.
	    // 사용자를 찾을 수 없는 경우 IllegalArgumentException 발생
	    MeetUser meetUser = meetUserRepository.findByEmail(userEmail)
	                        .orElseThrow(() -> {
	                            log.error("MeetUser를 찾을 수 없습니다: {}", userEmail);
	                            return new ResourceNotFoundException("User를 찾을 수 없습니다: " + userEmail); 
	                        });
	    
	    Long userId = meetUser.getId(); // MeetUser의 ID(PK) 획득
	    Optional<UserSocialToken> socialTokenOpt = userSocialTokenRepository.findByUser_IdAndProvider(userId, provider);
	    if (socialTokenOpt.isEmpty()) {
	        log.warn("사용자 ID {} (Email: {})에 대한 {} Refresh Token이 DB에 없습니다. Google 연동이 필요합니다.", 
	                 userId, userEmail, provider);
	        // [ 3. 수정 ] (400 에러 원인) IllegalArgumentException 대신 GoogleOAuthException 사용 (401 유발)
	        throw new GoogleOAuthException(provider + " 연동 토큰이 없습니다. 먼저 계정 연동을 완료해야 합니다.");
	    }

	    UserSocialToken socialToken = socialTokenOpt.get();
	    String refreshToken = socialToken.getRefreshToken();
	  
	    // 만료 시간 체크 (현재 시간보다 5분 이상 남았으면 갱신 안 함)
	    if (socialToken.getExpiresAt() != null && socialToken.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(5))) {
            log.debug("Access Token이 유효하여 갱신 없이 반환합니다.");
	        return socialToken.getAccessToken();
	    }
	    log.debug("Access Token이 만료되었거나 만료 직전이므로 갱신을 시도합니다.");
	    return this.refreshAccessToken(refreshToken, provider.toLowerCase(),userId, socialToken);
	}

	 private String refreshAccessToken(String refreshToken, String provider, Long userId, UserSocialToken socialToken) {
	    	
	    	GoogleAuthDTO.ProviderConfig config = googleAuthDTO.getProvider()
	                .get(provider);   	
	    	
	        log.info("API CALL: {} 토큰 엔드포인트로 Refresh Token 갱신 요청 시도. URI: {}", provider, config.getTokenEndpoint());
	        try {
	            GoogleTokenDto tokenResponse = webClient.post()
	                    .uri(config.getTokenEndpoint())
	                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
	                    .body(BodyInserters.fromFormData("client_id", config.getClientId())
	                            .with("client_secret", config.getClientSecret())
	                            .with("refresh_token", refreshToken)
	                            .with("grant_type", "refresh_token"))
	                    .retrieve()
	                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), clientResponse -> {
	                    	return clientResponse.bodyToMono(String.class)
	                                .flatMap(body -> {
	                                    log.error("Google Token API 에러 응답 수신. 상태: {}, 바디: {}", 
	                                               clientResponse.statusCode(), body);
	                                    if (body.contains("invalid_grant")) {
	                                        // 무효화된 토큰 DB에서 삭제
	                                        userSocialTokenRepository.findByRefreshToken(refreshToken)
	                                                .ifPresent(token -> {
	                                                    log.warn("무효화된 Refresh Token 삭제 (User ID: {}): {}", userId, token.getRefreshToken().subSequence(0, 10));
	                                                    userSocialTokenRepository.delete(token);
	                                                });
	                                    }                              
	                                    
	                                    // [ 4. 수정 ] (500 에러 원인) RuntimeException 대신 GoogleOAuthException 사용 (401 유발)
	                                    return Mono.error(new GoogleOAuthException("Google 토큰 갱신 실패. 응답: " + body));
	                                });
	                    })
	                .bodyToMono(GoogleTokenDto.class) 
	                .block(); // WebClient 동기 사용
	            
	            log.info("API RESPONSE: {} Access Token 갱신 성공. 만료 시간: {}초", provider, tokenResponse.getExpiresIn());
	            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
	                // [ 5. 수정 ] GoogleOAuthException 사용 (401 유발)
	                throw new GoogleOAuthException("Google로부터 유효한 Access Token을 받지 못했습니다.");
	            }     
	            
	            String newAccessToken = tokenResponse.getAccessToken();
	            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn());
	           
	            socialToken.setAccessToken(newAccessToken);
	            socialToken.setExpiresAt(expiresAt);
	            userSocialTokenRepository.save(socialToken); //DB 갱신
	            log.info("Access Token 갱신 성공. 만료 시간: {}초", tokenResponse.getExpiresIn());
	            return newAccessToken;

	        } catch (GoogleOAuthException e) { // 내가 던진 예외는 그대로 다시 던짐
	            log.warn("GoogleOAuthException 발생: {}", e.getMessage());
	            throw e; 
	        } catch (Exception e) {
	            log.error("Google Access Token 갱신 중 일반 오류 발생", e);
	            // [ 6. 수정 ] 그 외 모든 예외도 500 대신 401로 유도 (네트워크 문제 등)
	            throw new GoogleOAuthException("Google Access Token 갱신 중 통신/처리 오류 발생: " + e.getMessage());
	        }
	    }
	
}