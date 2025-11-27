package com.dialog.token.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.dialog.exception.UserNotFoundException;
import com.dialog.security.jwt.JwtTokenProvider;
import com.dialog.token.domain.RefreshToken;
import com.dialog.token.domain.RefreshTokenDto;
import com.dialog.token.domain.UserSocialToken;
import com.dialog.user.domain.MeetUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenReissueService {

    private final RefreshTokenServiceImpl refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;

    public String reissueAccessToken(String refreshToken) {
        RefreshTokenDto tokenDto = refreshTokenService.getValidRefreshTokenDto(refreshToken);
        MeetUser user = tokenDto.getUser();
        
        if (user == null) {
            throw new UserNotFoundException("리프레시 토큰에 연결된 사용자를 찾을 수 없습니다.");
        }
    
        UserDetails userDetails = new User(user.getEmail(), "", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        // UserDetails 기반 Authentication 생성
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        // 새 accessToken 생성
        String newAccessToken = jwtTokenProvider.createToken(authentication);

        return newAccessToken;
    }
}