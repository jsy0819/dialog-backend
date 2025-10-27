package com.dialog.user.service;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.dialog.user.domain.MeetUser;

public class CustomUserDetails implements UserDetails {
    private Long id; // DB 회원 고유 ID
    private String username; // 이메일(로그인 아이디)
    private String password; // 암호화된 비밀번호
    private Collection<? extends GrantedAuthority> authorities; // 권한 목록

    // 생성자: MeetUser 엔티티를 받아 필드 초기화
    public CustomUserDetails(MeetUser user) {
        this.id = user.getId(); // DB PK 저장
        this.username = user.getEmail();
        this.password = user.getPassword();
        // 권한은 임시로 ROLE_USER 단일 권한 할당(필요시 user 권한 정보 사용 가능)
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    // 사용자 ID를 가져오는 커스텀 getter (컨트롤러 등에서 사용 가능)
    public Long getId() {
        return id;
    }

    // 아래는 UserDetails 인터페이스 구현 메서드들

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
    @Override
    public String getPassword() {
        return password;
    }
    @Override
    public String getUsername() {
        return username;
    }

    // 계정 만료 여부 (false면 만료)
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    // 계정 잠금 여부 (false면 잠김)
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    // 자격 증명(비밀번호) 만료 여부 (false면 비밀번호 만료)
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    // 계정 활성화 여부 (false면 비활성화)
    @Override
    public boolean isEnabled() {
        return true;
    }
}
