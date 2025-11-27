package com.dialog.security.oauth2;

import java.util.Map;

import com.dialog.exception.SocialUserInfoException;

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
				throw new SocialUserInfoException("지원하지 않는 소셜 로그인 공급자입니다: " + registId);
			}
	}
	
		
}

