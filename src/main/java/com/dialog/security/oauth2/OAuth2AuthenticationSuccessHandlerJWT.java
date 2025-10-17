package com.dialog.security.oauth2;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.dialog.security.jwt.JwtTokenProvider;

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
	
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
	        throws IOException, ServletException {

	    log.info("=== 소셜 로그인 JWT 발급 성공 ===");

	    try {
	        // JWT 토큰 생성
	        String token = jwtTokenProvider.createToken(authentication);

	        // JWT를 쿠키에 저장
	        Cookie jwtCookie = new Cookie("jwt", token);
	        jwtCookie.setPath("/");
	        jwtCookie.setHttpOnly(false);  // JS 접근 가능 (운영 환경에서는 true 권장)
	        jwtCookie.setSecure(false);
	        jwtCookie.setMaxAge(60 * 60 * 24); // 1일
	        response.addCookie(jwtCookie);
	        log.info("서버에서 JWT 쿠키 설정 완료");

	        // 프론트엔드 메인페이지로 바로 리다이렉트
	        // 추후 실제 사용 URL 로 변경시 secret.yml 작업 필요
	        String redirectUrl = "http://localhost:5500/home.html";
	        getRedirectStrategy().sendRedirect(request, response, redirectUrl);

	    } catch (IOException e) {
	        log.error("리다이렉트 중 IO 오류 발생: {}", e.getMessage(), e);
	        // 필요 시 대체 처리 (로그인 페이지 리다이렉트, 오류 페이지 등)//
	        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Redirect 의 실패했습니다. ");
	    } catch (RuntimeException e) {
	        log.error("JWT 생성 중 오류: {}", e.getMessage(), e);
	        response.sendRedirect("/login?error=jwt_error");
	    } catch (Exception e) {
	        log.error("알 수 없는 오류 발생: {}", e.getMessage(), e);
	        response.sendRedirect("/login?error=jwt_error");
	    }
	}
	
}
