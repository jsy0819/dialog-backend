package com.dialog.token.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dialog.token.service.SocialTokenService;

import groovy.util.logging.Slf4j;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class OAuth2Controller {

    private final SocialTokenService socialTokenService;

    @PostMapping("/api/oauth2/google/token")
    public ResponseEntity<?> refreshToken(@RequestParam String userEmail) {
        // 유저 이메일로 AccessToken 재발급 시도
        try {
            String accessToken = socialTokenService.getToken(userEmail, "google");
            return ResponseEntity.ok(Map.of("accessToken", accessToken));
        } catch (RuntimeException e) {
            // Refresh Token 만료 등 예외 발생 시
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
}