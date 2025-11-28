package com.dialog.googleauth.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dialog.exception.UserNotFoundException;
import com.dialog.googleauth.service.GoogleAuthService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class GoogleAuthController {

    private final GoogleAuthService googleAuthService;
    
    @Value("${app.frontend-url}")
    private String frontendUrl;
    
    @GetMapping("/api/calendar/link/start")
    public ResponseEntity<?> startGoogleAccountLink(Authentication authentication) {
        // [수정] Principal이 null인지만 체크 (UserDetails 타입 강제 제거 - JWT 인증 시 String 타입일 수 있음)
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증이 필요합니다."));
        }

        try {
            Object principal = authentication.getPrincipal();
            Long userId;
            
            // [수정] Principal 타입에 따라 분기 처리 (일반/카카오 로그인 사용자도 Google 캘린더 연동 가능하도록)
            if (principal instanceof UserDetails userDetails) {
                userId = googleAuthService.extractUserId(userDetails);
            } else if (principal instanceof String email) {
                userId = googleAuthService.extractUserIdByEmail(email);
            } else {
                return ResponseEntity.status(401).body(Map.of("error", "지원하지 않는 인증 타입입니다."));
            }
                        
            String authUrl = googleAuthService.generateAuthUrl(userId);
            return ResponseEntity.ok(Map.of("authUrl", authUrl));
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
        	e.printStackTrace(); 
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류: " + e.getMessage()));
        }
    }

    @GetMapping("/auth/google/link/callback")
    public void handleGoogleLinkCallback(@RequestParam("code") String code, 
                                         @RequestParam("state") String state,
                                         HttpServletResponse response) throws IOException {
        Long userId;
        try {
            userId = Long.parseLong(new String(Base64.getUrlDecoder().decode(state)));
        } catch (Exception e) {
        	response.sendRedirect(frontendUrl + "/error.html?message=invalid_state");
            return;
        }

        try {
            googleAuthService.exchangeCodeAndSaveToken(userId, code);
            response.sendRedirect(frontendUrl + "/home.html?link=success");
        } catch (UserNotFoundException e) {
            response.sendRedirect("/error-page?message=user_not_found");
        } catch (IOException e) {
            response.sendRedirect("/error-page?message=token_exchange_failed");
        }
    }
}