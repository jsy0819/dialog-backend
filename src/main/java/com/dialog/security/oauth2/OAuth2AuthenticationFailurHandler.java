package com.dialog.security.oauth2;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;


import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class OAuth2AuthenticationFailurHandler implements AuthenticationFailureHandler {
	
	private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
	
    @Value("${app.oauth2.fail-uri}")
    String failUrl;

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, 
										HttpServletResponse response,
										AuthenticationException exception) throws IOException, ServletException {
		
		String errMsg = "소셜 로그인 실패!";
		
		// AuthenticationException : 시큐리티 에서 정의한 인증 과정중에 발생하는 모든 예외의 기본클래스.
		// 현재 예외의 원인이 된 그 예외 자체를 리턴해주는 메서드. 
	    if (exception.getCause() != null) {
	           errMsg += " 원인: " + exception.getCause().getMessage();
	       }
	       request.getSession().setAttribute("socialErrorMessage", errMsg);
	       
	       // 실패했더라도 임시 쿠키는 삭제
	       authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
		   // 로그인 페이지로 리다이렉트
	       response.sendRedirect(failUrl);

				
	}

}
