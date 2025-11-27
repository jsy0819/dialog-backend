package com.dialog.token.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dialog.exception.GoogleOAuthException;
import com.dialog.token.service.SocialTokenService;

import groovy.util.logging.Slf4j;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/oauth2")
@RequiredArgsConstructor
@Slf4j
public class OAuth2Controller {

    private final SocialTokenService socialTokenService;

    @PostMapping("/google/token")
    public ResponseEntity<?> refreshToken(@RequestParam String userEmail) {
        try {
            // 유저 이메일로 AccessToken 재발급 시도
            String accessToken = socialTokenService.getToken(userEmail, "google");
            return ResponseEntity.ok(Map.of("accessToken", accessToken));
        } catch (GoogleOAuthException e) {
            // 커스텀 예외 처리 - 만료 등 OAuth 관련 오류
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (RuntimeException e) {
            // 그 외 예외도 UNAUTHORIZED 처리 권장
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
}