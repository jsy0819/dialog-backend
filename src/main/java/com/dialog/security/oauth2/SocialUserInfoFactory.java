package com.dialog.security.oauth2;

import java.util.Map;

public class SocialUserInfoFactory {
	
	// 소셜로그인 관련
	public static SocialUserInfo getSocialUserInfo(String registId,
			Map<String, Object> attributes) {
		switch (registId.toLowerCase()) {
			case "google": {
				return new GoogleUserInfo(attributes);				
			}
			case "kakao": {
				return new KaKaoUserInfo(attributes);
			}
			default:
				throw new IllegalArgumentException("Unexpected value: " + registId);
			}
	}
	
		
}

