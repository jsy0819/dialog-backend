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
	     Map<String, Object> result = new HashMap<>();
	     try {
	         // 서비스 계층으로 회원가입 시도 (DB 저장)
	         meetuserService.signup(dto);  
	         result.put("success", true);
	         result.put("message", "회원가입 성공");
	         return ResponseEntity.ok(result);
	     } catch (IllegalStateException | IllegalArgumentException e) {
	         result.put("success", false);
	         result.put("message", e.getMessage());
	         return ResponseEntity.badRequest().body(result);
	     }
	 }
	
	 // 2. 로그인
	 @PostMapping(value = "/api/auth/login", produces = MediaType.APPLICATION_JSON_VALUE)
	 public ResponseEntity<?> login(@RequestBody LoginDto dto) {
	     Map<String, Object> result = new HashMap<>();
	     try {
	         // 1. 클라이언트가 전달한 이메일과 비밀번호로 사용자 인증 수행
	         MeetUser user = meetuserService.login(dto.getEmail(), dto.getPassword());
	
	         // 2. Spring Security 인증 객체 생성
	         Authentication authentication = new UsernamePasswordAuthenticationToken(
	        		    user.getEmail(), null, 
	        		    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
    		 );
	
	         // 3. 인증 정보를 기반으로 JWT 액세스 토큰 생성
	         String token = jwtTokenProvider.createToken(authentication);
	
	         // 4. 리프레시 토큰 생성 및 DTO 변환 - 사용자 정보와 연동하여 DB에 저장됨
	         RefreshTokenDto refreshTokenDto = refreshTokenService.createRefreshTokenDto(user);
	
	         // 5. 클라이언트에게 반환할 응답 데이터 구성
	         result.put("success", true);  // 처리 성공 상태
	         result.put("token", token);   // 새로 발급된 JWT 액세스 토큰
	         result.put("refreshToken", refreshTokenDto.getRefreshToken());           // 리프레시 토큰 문자열
	         result.put("refreshTokenExpiresAt", refreshTokenDto.getExpiresAt());     // 리프레시 토큰 만료시간
	         result.put("message", "로그인 성공");
	         // 사용자 기본 정보(name, email) JSON 형태로 포함
	         result.put("user", Map.of(
	                 "name", user.getName(),
	                 "email", user.getEmail(),
	                 "role", user.getRole().name()
	             ));
	
	         // 6. 성공 응답으로 클라이언트에 JSON 반환
	         return ResponseEntity.ok(result);
	     } catch (IllegalStateException e) {
	         // 7. 인증 실패 시 에러 응답 생성
	         result.put("success", false);
	         result.put("message", e.getMessage());
	         return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
	     }
	 }
	
	 //3.현재 로그인된 사용자 정보 조회
	 @GetMapping("/api/auth/me")
	 public ResponseEntity<?> getCurrentUserInfo(Authentication authentication) {
	     MeetUserDto dto = meetuserService.getCurrentUser(authentication);
	     // name, email 등 필요한 필드를 JSON으로 반환
	     return ResponseEntity.ok(Map.of(
	         "name", dto.getName(),
	         "email", dto.getEmail(),
	         "role", dto.getRole()
	     ));	     
	 }

	// 4. 설정 페이지에서 사용자 정보(직무/직급) 업데이트
	@PutMapping("/api/user/settings")
	public ResponseEntity<?> updateUserSettings(Authentication authentication, 
			@Valid @RequestBody UserSettingsUpdateDto dto 
	) {
		Map<String, Object> result = new HashMap<>();
		try {
			// 1. 인증 정보가 없으면 거부
			if (authentication == null || !authentication.isAuthenticated()) {
				result.put("success", false);
				result.put("message", "인증되지 않은 사용자입니다.");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
			}

			// 2. 서비스 계층에 인증정보와 수정할 DTO를 넘겨 업데이트 위임
			meetuserService.updateUserSettings(authentication, dto);

			// 3. 성공 응답 반환
			result.put("success", true);
			result.put("message", "개인정보가 성공적으로 저장되었습니다.");
			return ResponseEntity.ok(result);

		} catch (Exception e) {
			// 4. 실패시 에러 응답
			result.put("success", false);
			result.put("message", e.getMessage());
			return ResponseEntity.badRequest().body(result);
		}
	}

}
