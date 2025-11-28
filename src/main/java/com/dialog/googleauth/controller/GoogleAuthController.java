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
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails)) {
            return ResponseEntity.status(401).body(Map.of("error", "인증이 필요합니다."));
        }

        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Long userId = googleAuthService.extractUserId(userDetails);
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