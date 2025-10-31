package com.dialog.GoogleAuth.DTO;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
//@Component
//@ConfigurationProperties(prefix = "google")
@ConfigurationProperties(prefix = "oauth2")
public class GoogleAuthDTO {
	
	private Map<String, ProviderConfig> provider;
	
	@Data
    public static class ProviderConfig {
        // TokenManagerService에서 필요한 필드만 남깁니다.
        private String clientId;
        private String clientSecret;
        private String tokenEndpoint; // 갱신 요청을 위한 Endpoint 추가
        private String redirectUri;
        private String authUri;
        private String scope; 
    }
}
