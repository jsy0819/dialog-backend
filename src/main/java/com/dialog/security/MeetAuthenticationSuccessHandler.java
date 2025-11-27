package com.dialog.security;

import java.io.IOException;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.dialog.security.oauth2.OAuth2AuthenticationSuccessHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MeetAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
	
	@Value("${app.oauth2.redirect-uri}")
	String redirectUrl ;
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, 
	                                    HttpServletResponse response,
	                                    Authentication authentication) throws IOException {
	      
	 log.info("로그인 성공: " + authentication.getName() + "님 환영합니다!(이 메시지는 success 핸들러입니다)");
	 
	// 추후 관리자 로그인시 관리자 페이지로 바로 이동되게 설정할 경우 사용
//	 Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();	 	 
//	 for (GrantedAuthority authority : authorities) {
//	     if (authority.getAuthority().equals("ROLE_ADMIN")) {
//	     // url 주소 추후 admin-dashboard.html 수정필요
//	     response.sendRedirect(redirectUrl);  // -> 관리자 페이지 URL 로 변경해야함.
//         return;
//	     }
//	 }
//	  
	 // 기본 사용자는 메인으로
	 response.sendRedirect(redirectUrl);
	 }

}

