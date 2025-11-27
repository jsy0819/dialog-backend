package com.dialog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
	/**
	 * WebClient 인스턴스를 Spring Bean으로 등록합니다. GoogleCalendarApiClient를 포함한 모든 API 호출에
	 * 사용됩니다.
	 */
	@Bean
	public WebClient webClient() {
		return WebClient.builder().build();
	}
}
