package com.dialog.security.oauth2;

// 소셜 로그인 관련
public interface SocialUserInfo {

    String getId();
    String getName();
    String getEmail();
    String getProfileImageUrl();
		
}