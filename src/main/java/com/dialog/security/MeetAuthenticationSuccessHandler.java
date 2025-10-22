package com.dialog.security;

import java.io.IOException;
import java.util.Collection;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.dialog.security.oauth2.OAuth2AuthenticationSuccessHandlerJWT;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MeetAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, 
                                    HttpServletResponse response,
                                    Authentication authentication) throws IOException {
      
	  log.info("로그인 성공: " + authentication.getName() + "님 환영합니다!(이 메시지는 success 핸들러입니다)");
      
      Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
      
      for (GrantedAuthority authority : authorities) {
          if (authority.getAuthority().equals("ROLE_ADMIN")) {
              // url 주소 추후 admin-dashboard.html 수정필요
              response.sendRedirect("/");
              return;
          }
      }
      
      // 기본 사용자는 메인으로
      response.sendRedirect("/");
  }

}

