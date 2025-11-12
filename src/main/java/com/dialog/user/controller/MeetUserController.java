package com.dialog.user.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dialog.exception.UserRoleAccessDeniedException;
import com.dialog.security.jwt.JwtTokenProvider;
import com.dialog.security.oauth2.SocialUserInfo;
import com.dialog.security.oauth2.SocialUserInfoFactory;
import com.dialog.token.domain.RefreshTokenDto;
import com.dialog.token.service.RefreshTokenServiceImpl;
import com.dialog.user.domain.LoginDto;
import com.dialog.user.domain.MeetUser;
import com.dialog.user.domain.MeetUserDto;
import com.dialog.user.domain.UserSettingsUpdateDto;
import com.dialog.user.service.MeetuserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController 
@RequiredArgsConstructor
public class MeetUserController {

	private final MeetuserService meetuserService;
	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenServiceImpl refreshTokenService;

    // 1. 회원가입
    @PostMapping("/api/auth/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody MeetUserDto dto) {
        // 클라이언트로부터 회원가입 요청 데이터(dto)를 받음 (JSON → MeetUserDto 변환)
        // Service 계층의 signup 메서드 호출하여 회원가입 처리
        // (가입정보 유효성 검증, 중복체크, 비밀번호 암호화, DB 저장 포함)
        meetuserService.signup(dto);

        // 회원가입 성공 메시지와 함께 HTTP 200 OK 응답 반환
        return ResponseEntity.ok(Map.of("success", true, "message", "회원가입 성공"));
    }

    // 2. 로그인
    @PostMapping(value = "/api/auth/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> login(@RequestBody LoginDto dto, HttpServletResponse response) {
        // 클라이언트로부터 로그인 요청 데이터(email, password)를 받음
        // Service 계층 login() 호출하여 사용자 인증 및 검증 수행
        // (DB에서 사용자 조회, 비밀번호 검증, 활성화 상태 확인)
        MeetUser user = meetuserService.login(dto.getEmail(), dto.getPassword());

        // 인증 정보를 토대로 Authentication 객체 생성 (Spring Security 내 권한정보 포함)
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            user.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );

        // JWT 토큰 생성
        String token = jwtTokenProvider.createToken(authentication);

        // JWT 토큰을 HTTP Only 쿠키에 저장하여 클라이언트에 전달
        Cookie jwtCookie = new Cookie("jwt", token);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(60 * 60 * 3); // 3시간 유효
        jwtCookie.setSecure(false);
        response.addCookie(jwtCookie);

        // Refresh Token 생성 및 반환할 DTO 생성
        RefreshTokenDto refreshTokenDto = refreshTokenService.createRefreshTokenDto(user);

        // 로그인 성공 정보와 토큰, 사용자 정보 등을 JSON 응답의 형태로 반환
        return ResponseEntity.ok(Map.of(
            "success", true,
            "token", token,
            "refreshToken", refreshTokenDto.getRefreshToken(),
            "refreshTokenExpiresAt", refreshTokenDto.getExpiresAt(),
            "message", "로그인 성공",
            "user", Map.of(
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole().name()
            )
        ));
    }

    // 3. 현재 로그인된 사용자 정보 조회
    @GetMapping("/api/auth/me")
    public ResponseEntity<?> getCurrentUserInfo(Authentication authentication) {
        // 현재 인증된 사용자 인증 정보를 파라미터로 받아서,
        // 서비스 계층 getCurrentUser() 호출해 사용자 상세정보 조회
        MeetUserDto dto = meetuserService.getCurrentUser(authentication);

        // 사용자 이름, 이메일, 역할 정보를 JSON 형태로 응답 반환
        return ResponseEntity.ok(Map.of(
            "name", dto.getName(),
            "email", dto.getEmail(),
            "role", dto.getRole()
        ));
    }

    // 4. 설정 페이지에서 사용자 정보(직무/직급) 업데이트
    @PutMapping("/api/user/settings")
    public ResponseEntity<?> updateUserSettings(Authentication authentication, 
                                                @Valid @RequestBody UserSettingsUpdateDto dto) {
        // 인증 정보 유무 확인, 인증 안 된 경우 커스텀 예외 던짐
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UserRoleAccessDeniedException("인증되지 않은 사용자입니다.");
        }

        // 사용자 인증 정보를 바탕으로 서비스 계층에 업데이트 작업 위임
        meetuserService.updateUserSettings(authentication, dto);

        // 성공 메시지를 포함한 HTTP 200 OK 응답 반환
        return ResponseEntity.ok(Map.of("success", true, "message", "개인정보가 성공적으로 저장되었습니다."));
    }
}