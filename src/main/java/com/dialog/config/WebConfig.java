package com.dialog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**") // /api로 시작하는 모든 경로에 대해 CORS 적용
				.allowedOrigins("http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501",
						"http://127.0.0.1:5501") // 허용할 프론트엔드 출처 지정.
				// 실제 사용할 HTTP 메서드만 명시(예: GET, POST 등). *대신 "*"이면 모두 허용
				.allowedMethods("*") // 모든 HTTP 메서드 허용 ("GET", "POST", "PUT" 등). 운영환경에선 보안 위해 필요한 메서드만 지정 권장
				.allowCredentials(true); // 인증정보(쿠키, 인증 헤더 등) 포함 요청 허용. OAuth/JWT/SameSite 세션 등에 필요
	}
}