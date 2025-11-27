package com.dialog.token.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dialog.exception.RefreshTokenException;
import com.dialog.global.utill.CookieUtil;
import com.dialog.token.service.TokenReissueService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;


@RestController
@RequiredArgsConstructor
public class TokenController {
   
    private final TokenReissueService tokenReissueService;
    private final CookieUtil cookieUtil;

   
   // 토큰 재발급 메서드
    @PostMapping("/api/reissue")
    public ResponseEntity<?> reissueAccessToken(@CookieValue(name = "refreshToken", required = false) String refreshToken,
                                                HttpServletResponse response) {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token is missing.");
        }

        try {
            // 서비스에서 새로운 Access Token 문자열 받기
            String newAccessToken = tokenReissueService.reissueAccessToken(refreshToken);
            // CookieUtil을 사용하여 쿠키 생성 및 응답에 추가
            response.addCookie(cookieUtil.createAccessTokenCookie(newAccessToken));

            return ResponseEntity.ok("Access token 재발급");
        } catch (RefreshTokenException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 리프레쉬 토큰 입니다.");
        }
    }
}
