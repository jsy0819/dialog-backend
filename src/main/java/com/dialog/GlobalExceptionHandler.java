package com.dialog;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.dialog.exception.GoogleOAuthException;
import com.dialog.exception.GoogleOAuthException.AccessDeniedException;
import com.dialog.exception.GoogleOAuthException.ResourceNotFoundException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;

// 모든 REST API 예외를 한 곳에서 처리하는 글로벌 예외 핸들러 클래스
@Slf4j
@RestControllerAdvice // 모든 REST 컨트롤러의 예외를 공통 처리
public class GlobalExceptionHandler {

	@ExceptionHandler(GoogleOAuthException.class)
    public ResponseEntity<Map<String, String>> handleGoogleOAuthException(GoogleOAuthException e) {
        log.warn("⚠️ Google OAuth Error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("errorCode", "GOOGLE_REAUTH_REQUIRED", "message", e.getMessage()));
    }

	// 2. 리소스 찾기 실패 (404)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFound(ResourceNotFoundException e) {
        log.warn("🔍 Resource Not Found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Not Found", "message", e.getMessage()));
    }
    
    // 3. 접근 거부 (403)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException e) {
         log.warn("⛔ Access Denied: {}", e.getMessage());
         return ResponseEntity.status(HttpStatus.FORBIDDEN)
                 .body(Map.of("error", "Forbidden", "message", e.getMessage()));
    }
    
    // 4. 잘못된 요청 (400) - 기존 로직 유지하되 더 깔끔하게
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException e) {
        log.warn("❌ Bad Request: {}", e.getMessage());
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
    
    // 5. 그 외 서버 에러 (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        log.error("🔥 Internal Server Error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal Server Error", "message", "서버 내부 오류가 발생했습니다."));
    }
}