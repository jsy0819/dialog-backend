package com.dialog.security.oauth2;

import java.util.Map;

public class GoogleUserInfo implements SocialUserInfo {

	private final Map<String, Object> attributes;
		
	public GoogleUserInfo(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	@Override
	public String getId() {
		// Google OIDC 표준에 따라 사용자 고유 ID (sub)를 리턴
		return (String) attributes.get("sub");
	}

	@Override
	public String getName() {
	    String name = (String) attributes.get("name");
	    // null 또는 빈 문자열일 경우 DB 저장을 위해 기본값 설정
	    return (name != null && !name.isEmpty()) ? name : "Google 사용자"; 
	}

	@Override
	public String getEmail() {
	    String email = (String) attributes.get("email");
	    // 이메일 값이 없으면 DB 저장을 위해 고유한 임시 이메일 제공
	    return (email != null && !email.isEmpty()) ? email : "google-no-email-" + getId() + "@dialog.com";
	}

	@Override
	public String getProfileImageUrl() {
		// Google이 제공하는 프로필 이미지 URL 키 (picture)
		return (String) attributes.get("picture");
	}
    
    @Override
    public String getProvider() {
        return "google";
    }

    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

}
