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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dialog.security.jwt.JwtTokenProvider;
import com.dialog.security.oauth2.SocialUserInfo;
import com.dialog.security.oauth2.SocialUserInfoFactory;
import com.dialog.user.domain.LoginDto;
import com.dialog.user.domain.MeetUser;
import com.dialog.user.domain.MeetUserDto;
import com.dialog.user.service.MeetuserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//사용자 REST API 컨트롤러: 회원가입/로그인 처리
@Slf4j
@RestController // REST(즉 JSON) 응답을 내려주는 컨트롤러임
@RequiredArgsConstructor // 생성자 주입 (meetuserService, jwtTokenProvider)
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500/"}) // CORS 허용 설정
public class MeetUserController {

 private final MeetuserService meetuserService; // 비즈니스 로직: 회원 DB, 인증 등
 private final JwtTokenProvider jwtTokenProvider; // JWT 토큰 발급 역할

 // 1. 회원가입 (클라이언트가 POST /api/auth/signup로 JSON 데이터 전송)
 @PostMapping("/api/auth/signup")
 public ResponseEntity<?> signup(@Valid @RequestBody MeetUserDto dto) {
     Map<String, Object> result = new HashMap<>();
     try {
         // 서비스 계층으로 회원가입 시도 (DB 저장)
         meetuserService.signup(dto);    // DB에 저장됨
         result.put("success", true);
         result.put("message", "회원가입 성공");
         // 성공 시: { success: true, message: ... } 형태의 JSON 반환
         return ResponseEntity.ok(result);
     } catch (IllegalStateException | IllegalArgumentException e) {
         // 중복/실패시: 예외를 잡아 메시지만 반환
         result.put("success", false);
         result.put("message", e.getMessage());
         return ResponseEntity.badRequest().body(result);
     }
 }

 // 2. 로그인 (클라이언트가 POST /api/auth/login로 이메일/비밀번호 JSON 전송)
 @PostMapping(value = "/api/auth/login", produces = MediaType.APPLICATION_JSON_VALUE)
 public ResponseEntity<?> login(@RequestBody LoginDto dto) {
     Map<String, Object> result = new HashMap<>();
     try {
         // 서비스 계층에서 인증 검증 (DB 조회 + 비밀번호 체크)
         MeetUser user = meetuserService.login(dto.getEmail(), dto.getPassword());
         // 인증 객체 생성 (Spring Security에서 권한 부여용)
         Authentication authentication = new UsernamePasswordAuthenticationToken(
             user.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
         );
         // JWT 토큰 발급 (이메일/권한 기반으로)
         String token = jwtTokenProvider.createToken(authentication);
         result.put("success", true);
         result.put("token", token);                 // 클라이언트에 JWT 제공
         result.put("message", "로그인 성공");
         log.info("Login API 응답 result: {}", result); // 서버 로그로 검증
         return ResponseEntity.ok(result);             // 성공 시: 토큰 포함하여 JSON 반환
     } catch (IllegalStateException e) {
         // 로그인 실패(계정X, 비번오류 등)시
         result.put("success", false);
         result.put("message", e.getMessage());
         return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
     }
 }

 // 3. (주석처리된 부분) 현재 로그인된 사용자 정보 조회
 // @GetMapping("/api/auth/me")
 // public ResponseEntity<?> getCurrentUserInfo(Authentication authentication) {
 //     MeetUserDto dto = meetuserService.getCurrentUser(authentication);
 //     return ResponseEntity.ok(dto);
 // }
}