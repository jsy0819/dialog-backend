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

//사용자 REST API 컨트롤러: 회원가입/로그인 처리
@Slf4j
@RestController // REST(즉 JSON) 응답을 내려주는 컨트롤러임
@RequiredArgsConstructor // 생성자 주입 (meetuserService, jwtTokenProvider)
//@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500/"}) // CORS 허용 설정
public class MeetUserController {

	 private final MeetuserService meetuserService; // 비즈니스 로직: 회원 DB, 인증 등
	 private final JwtTokenProvider jwtTokenProvider; // JWT 토큰 발급 역할
	 private final RefreshTokenServiceImpl refreshTokenService;

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
	         // 1. 클라이언트가 전달한 이메일과 비밀번호로 사용자 인증 수행
	         MeetUser user = meetuserService.login(dto.getEmail(), dto.getPassword());
	
	         // 2. Spring Security 인증 객체 생성 (사용자 이메일, 권한 정보 포함)
	         Authentication authentication = new UsernamePasswordAuthenticationToken(
	             user.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
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
	                 "email", user.getEmail()
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
	         "email", dto.getEmail()
	     ));	     
	 }

	// 4. '설정' 페이지에서 사용자 정보(직무/직급) 업데이트
	// '저장하기' 버튼 클릭 시 /api/user/settings 로 PUT 요청
	@PutMapping("/api/user/settings")
	public ResponseEntity<?> updateUserSettings(Authentication authentication, // (필수) 누가 요청했는지 (인증)
			@Valid @RequestBody UserSettingsUpdateDto dto // (필수) 수정할 내용 (job, position)
	) {
		Map<String, Object> result = new HashMap<>();
		try {
			// 1. 인증 정보가 없으면 거부 (혹시 모를 상황 대비)
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
			// 4. 실패(예: 사용자를 못찾음, 값 누락 등) 시 에러 응답
			result.put("success", false);
			result.put("message", e.getMessage());
			return ResponseEntity.badRequest().body(result);
		}
	}

}
