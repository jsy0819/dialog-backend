package com.dialog;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;


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
	
}
