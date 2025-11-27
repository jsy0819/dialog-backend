package com.dialog.user.service;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.dialog.user.domain.Job;
import com.dialog.user.domain.MeetUser;

public class CustomUserDetails implements UserDetails {
	
    private final MeetUser user;

    public CustomUserDetails(MeetUser user) {
        this.user = user;
    }
    
    public MeetUser getMeetUser() {
        return this.user;
    }
    
    // 직무 설정이 안 되어 있는지 확인
    public boolean isJobEmpty() {
        return this.user.getJob() == Job.NONE; 
    }

    public Long getId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String roleName = user.getRole() != null ? user.getRole().name() : "USER";
        return List.of(new SimpleGrantedAuthority("ROLE_" + roleName));
    }
    @Override
    public String getPassword() {
    	return user.getPassword(); 
    }
    
    @Override
    public String getUsername() { 
    	return user.getEmail(); 
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