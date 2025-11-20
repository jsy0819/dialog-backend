package com.dialog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.web.client.RestTemplate;  // HTTP 통신용 클라이언트

@Configuration
public class AppConfig {
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);  // 비밀번호 12자 이상
	}
	
	@Bean
	public OAuth2AuthorizationRequestResolver customAuthorizationRequestResolver(
	        ClientRegistrationRepository clientRegistrationRepository) {
	    DefaultOAuth2AuthorizationRequestResolver resolver =
	            new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");
	    resolver.setAuthorizationRequestCustomizer(customizer -> customizer
	            .additionalParameters(params -> {
	                params.put("access_type", "offline");
	                params.put("prompt", "consent");
	            })
	    );
	    return resolver;
	}
	
	// HTTP 통신용 클라이언트
	// Python FastAPI(포트 8000)와 통신하여 챗봇 요청 중계
	@Bean
	public RestTemplate restTemplate() {
	    return new RestTemplate();
	}
	
}
