package com.dialog.GoogleAuth.DTO;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "google")
public class GoogleAuthDTO {
	private String clientId;
    private String redirectUri;
    private String authUri;
    private String scope;
    private String clientSecret;
}
