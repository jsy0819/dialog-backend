package com.dialog.security.oauth2;

import java.util.Map;

// 소셜 로그인 관련
public interface SocialUserInfo {

    String getId();
    String getName();
    String getEmail();
    String getProfileImageUrl();
    String getProvider(); // 현재 로그인 공급자 이름 (e.g., "google", "kakao")
    Map<String, Object> getAttributes(); // 원본 사용자 속성 맵
		
}