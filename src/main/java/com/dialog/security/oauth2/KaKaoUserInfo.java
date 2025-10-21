package com.dialog.security.oauth2;

import java.util.Map;

public class KaKaoUserInfo implements SocialUserInfo {
	
	private final Map<String, Object> attributes;
	
	public KaKaoUserInfo(Map<String, Object> attributes) {
		this.attributes = attributes;
	}
	// 아이디 가져오는 메서드 
	@Override
    public String getId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getName() {
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
        if (properties == null) return "카카오 사용자";
        return (String) properties.get("nickname");
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getEmail() {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        if (kakaoAccount == null) return null;
        return (String) kakaoAccount.get("email");
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getProfileImageUrl() {
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
        if (properties == null) return null;
        return (String) properties.get("profile_image");
    }
    @Override
	public String getProvider() {
		// 소셜 로그인 제공자는 "kakao" 입니다.
		return "kakao";
	}
	
	@Override
	public Map<String, Object> getAttributes() {
		// 원본 사용자 정보(attributes)를 반환합니다.
		return this.attributes;
	}

}
