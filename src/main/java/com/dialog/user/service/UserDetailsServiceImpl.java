package com.dialog.user.service;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.dialog.user.domain.MeetUser;
import com.dialog.user.repository.MeetUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final MeetUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // 1. 사용자명으로 DB에서 사용자 엔티티를 조회함
        MeetUser meetuser = userRepository.findByEmail(username)
          .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다. " + username));

        // 2. UserDetails 객체 생성하여 Spring Security 인증 프로세스에 맞게 사용자 정보를 매핑
        return User.builder()
          .username(meetuser.getEmail()) // 사용자명 지정
          .password(meetuser.getPassword()) // 암호화된 비밀번호 할당
          .authorities(getAuthorities(meetuser)) // 권한 정보 할당
          // 향후 계정 상태 관리에 따라 아래 설정 추가 가능
//          .accountExpired(meetuser.isAccountExpired())
//          .accountLocked(meetuser.isAccountLocked())
//          .credentialsExpired(meetuser.isCredentialsExpired())
//          .disabled(meetuser.isDisabled())
          .build();
    }

    // 권한 정보를 Spring Security의 GrantedAuthority 컬렉션으로 변환
    private Collection<? extends GrantedAuthority> getAuthorities(MeetUser meetuser) {
        // 현재는 고정된 "ROLE_USER" 권한만 단일로 반환
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
}

