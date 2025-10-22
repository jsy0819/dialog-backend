package com.dialog.security.oauth2;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.dialog.security.jwt.JwtTokenProvider;
import com.dialog.token.domain.RefreshTokenDto;
import com.dialog.token.service.RefreshTokenServiceImpl;
import com.dialog.user.domain.MeetUser;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandlerJWT extends SimpleUrlAuthenticationSuccessHandler{
	
	//SimpleUrlAuthenticationSuccessHandler : 
	// AuthenticationSuccessHandler의 구현체. 
	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenServiceImpl refreshTokenService;
	
    @Value("${app.oauth2.redirect-uri}")
    String redirectUrl ;
	
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
	        throws IOException, ServletException {
		
		

	    log.info("=== 소셜 로그인 JWT 발급 성공 ===");

	    try {
	        // 1. 액세스 토큰 생성
	        String accessToken = jwtTokenProvider.createToken(authentication);	        
	        log.info("발급된 액세스 토큰: {}", accessToken);
	        

	        // 2. 사용자 정보 추출 (CustomOAuth2User 가정)
	        CustomOAuth2User customUser = (CustomOAuth2User) authentication.getPrincipal();
	        MeetUser user = customUser.getMeetuser();

	        // 3. 리프레시 토큰 생성 및 DTO 획득
	        RefreshTokenDto refreshTokenDto = refreshTokenService.createRefreshTokenDto(user);
	        String refreshToken = refreshTokenDto.getRefreshToken();  // 토큰 문자열
	        LocalDateTime expiresAt = refreshTokenDto.getExpiresAt(); // 만료 시간
	        log.info("발급된 리프레시 토큰: {}", refreshToken);
	        log.info("리프레시 토큰 만료 시각: {}", expiresAt);

	        // 4. 액세스 토큰 쿠키 설정
	        Cookie accessTokenCookie = new Cookie("jwt", accessToken);
	        accessTokenCookie.setPath("/");
	        accessTokenCookie.setHttpOnly(true);  // JS 접근 차단
	        accessTokenCookie.setSecure(true);    // HTTPS 환경에서 true 
	        accessTokenCookie.setMaxAge(60 * 60 * 24); // 1일
	        response.addCookie(accessTokenCookie);

	        // 5. 리프레시 토큰 쿠키 설정
	        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshTokenDto.getRefreshToken());
	        refreshTokenCookie.setPath("/");
	        refreshTokenCookie.setHttpOnly(true);  // JS 접근 차단
	        refreshTokenCookie.setSecure(true);
	        // 리프레시 토큰 만료시간까지 쿠키 유효기간 설정 (7일)
	        int refreshTokenMaxAge = (int) Duration.between(
	               LocalDateTime.now(), refreshTokenDto.getExpiresAt()).getSeconds();
	        refreshTokenCookie.setMaxAge(refreshTokenMaxAge);
	        response.addCookie(refreshTokenCookie);

	        log.info("액세스 토큰 및 리프레시 토큰 쿠키 설정 완료");

	        // 6. 프론트엔드 메인 페이지로 리다이렉트
	        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
	        log.info("리다이렉트 완료 : {}", redirectUrl);

	    } catch (IOException e) {
	        log.error("리다이렉트 중 IO 오류 발생: {}", e.getMessage(), e);
	        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Redirect 실패");
	    } catch (RuntimeException e) {
	        log.error("JWT 생성 중 오류: {}", e.getMessage(), e);
	        response.sendRedirect("/login");
	    } catch (Exception e) {
	        log.error("알 수 없는 오류 발생: {}", e.getMessage(), e);
	        response.sendRedirect("/login");
	    }
	}
}