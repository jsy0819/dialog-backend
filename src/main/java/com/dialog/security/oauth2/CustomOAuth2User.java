package com.dialog.security.oauth2;

import java.util.Collection;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.dialog.user.domain.MeetUser;



//CustomOAuth2User 클래스는 OAuth2 인증 과정을 거친 후 사용자 정보를 SecurityContext에
//담아 사용할 때 사용자 정보를 확장해 다루기 쉽게 커스텀한 객체입니다.

public class CustomOAuth2User implements OAuth2User {

 // 권한 정보 (ROLE_USER 등) 컬렉션
	private final Collection<? extends GrantedAuthority> authorities;
 
 // OAuth2 공급자가 제공하는 사용자의 원본 속성 데이터 (이메일, 이름 등)
	private final Map<String, Object> attributes;

 // 사용자 이름 키, 예: "sub", "id", "email" 등
	private final String nameAttributeKey;

 // 내부 도메인 사용자 엔티티
	private final MeetUser meetuser;

 // 생성자: 외부 OAuth2 정보와 내부 사용자 엔티티를 함께 묶음//
public CustomOAuth2User(Collection<? extends GrantedAuthority> authorities, Map<String, Object> attributes,
         String nameAttributeKey, MeetUser meetuser) {
     this.authorities = authorities;
     this.attributes = attributes;
     this.nameAttributeKey = nameAttributeKey;
     this.meetuser = meetuser;
 }

 // OAuth2 공급자로부터 받은 사용자 원본 속성 반환
 @Override
 public Map<String, Object> getAttributes() {
     return attributes;
 }

 // 사용자 권한 반환 (Security 인증 권한)
 @Override
 public Collection<? extends GrantedAuthority> getAuthorities() {
     return authorities;
 }

 // OAuth2 유저 식별자 반환
 @Override
 public String getName() {
     Object id = attributes.get("id");
     return (id != null) ? String.valueOf(id) : null;
 }

 // 내부 도메인 사용자 엔티티 Getter
 public MeetUser getMeetuser() {
     return meetuser;
 }

 // 도메인 유저 이름 편리 메서드
 public String getname() {
     return meetuser.getName();
 }

 // 도메인 유저 프로필 이미지 URL 편리 메서드
 public String getProfileImgUrl() {
     return meetuser.getProfileImgUrl();
 }
}
