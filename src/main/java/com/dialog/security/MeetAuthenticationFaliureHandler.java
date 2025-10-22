package com.dialog.security;

import java.io.IOException;

import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.dialog.security.oauth2.OAuth2AuthenticationSuccessHandlerJWT;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MeetAuthenticationFaliureHandler implements AuthenticationFailureHandler{

        @Override
        public void onAuthenticationFailure(HttpServletRequest request, 
                                          HttpServletResponse response,
                                          AuthenticationException exception) throws IOException {
            
        	log.info("로그인 실패: " + exception.getMessage() +"(이 메시지는 Failure 핸들러입니다)");
            
            // 실패 원인별 메시지 (선택사항)
            String errorMessage = "로그인에 실패했습니다!";
            
            // 시큐리티에서 제공해주는 예외처리.
            if (exception instanceof BadCredentialsException) {
                errorMessage = "아이디 또는 비밀번호가 올바르지 않습니다!";
            } else if (exception instanceof DisabledException) {
                errorMessage = "계정이 비활성화되었습니다! 관리자에게 문의해주세요.";
            } else if (exception instanceof AccountExpiredException) {
                errorMessage = "계정이 만료되었습니다! 갱신이 필요해요.";
            }
            
            // 세션에 에러 메시지 저장 후 리다이렉트
            request.getSession().setAttribute("errorMessage", errorMessage);
            response.sendRedirect("/login");
     }

    }
