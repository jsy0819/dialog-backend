package com.dialog.user.service;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.dialog.exception.UserNotFoundException;
import com.dialog.user.domain.MeetUser;
import com.dialog.user.repository.MeetUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final MeetUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 데이터베이스에서 이메일(username) 기준으로 사용자 엔티티 조회
        MeetUser meetuser = userRepository.findByEmail(username)
          .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. " + username));
      
        // 2. 커스텀 UserDetails 객체 생성하여 스프링 시큐리티가 인증에 사용할 수 있도록 반환
        return new CustomUserDetails(meetuser);
    }
}
