package com.dialog.GoogleAuth.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dialog.GoogleAuth.Service.GoogleAuthService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class GoogleAuthController {

    private final GoogleAuthService googleAuthService;

    /**
     * [신규 API] 카카오/서비스 계정 사용자가 Google 연동을 시작하기 위해 호출하는 API.
     * JWT 인증이 반드시 필요합니다.
     */
    @GetMapping("/api/calendar/link/start")
    public ResponseEntity<?> startGoogleAccountLink(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails)) {
            return ResponseEntity.status(401).body(Map.of("error", "인증이 필요합니다."));
        }

        // 1. JWT에서 현재 로그인된 사용자의 ID를 추출합니다.
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long userId = googleAuthService.extractUserId(userDetails);

        // 2. userId를 state에 담아 Google 인증 URL을 생성합니다.
        String authUrl = googleAuthService.generateAuthUrl(userId);

        // 3. 프론트엔드에 이 URL을 반환합니다. (프론트엔드는 이 URL로 리다이렉트)
        return ResponseEntity.ok(Map.of("authUrl", authUrl));
    }

    /**
     * [신규 콜백] Google Console에 등록해야 하는 새로운 Redirect URI.
     * Google 인증 성공 시 'code'와 'state'를 받습니다.
     */
    @GetMapping("/auth/google/link/callback")
    public void handleGoogleLinkCallback(@RequestParam("code") String code, 
                                         @RequestParam("state") String state,
                                         HttpServletResponse response) throws IOException {
        
        Long userId;
        try {
            // 1. state 값을 Base64 디코딩하여 원본 userId를 복원합니다.
            userId = Long.parseLong(new String(Base64.getUrlDecoder().decode(state)));
        } catch (Exception e) {
            // state가 잘못된 경우 (CSRF 공격 또는 오류)
            response.sendRedirect("/error-page?message=invalid_state");
            return;
        }

        // 2. 획득한 code와 userId로 토큰 교환 및 Refresh Token 저장
        googleAuthService.exchangeCodeAndSaveToken(userId, code);

        // 3. 연동 성공 후 사용자를 캘린더 페이지(또는 홈)로 리다이렉트
        response.sendRedirect("http://localhost:5500/home.html?link=success");
    }
}