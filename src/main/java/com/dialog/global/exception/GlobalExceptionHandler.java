package com.dialog.global.exception;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.authentication.DisabledException;

import com.dialog.exception.AccessDeniedException;
import com.dialog.exception.ChatbotApiException;
import com.dialog.exception.GoogleOAuthException;

import com.dialog.exception.InactiveUserException;
import com.dialog.exception.InvalidJwtTokenException;
import com.dialog.exception.InvalidPasswordException;
import com.dialog.exception.OAuthUserNotFoundException;
import com.dialog.exception.RefreshTokenException;
import com.dialog.exception.ResourceNotFoundException;
import com.dialog.exception.SocialUserInfoException;
import com.dialog.exception.SocialUserSaveException;
import com.dialog.exception.TermsNotAcceptedException;
import com.dialog.exception.UserAlreadyExistsException;
import com.dialog.exception.UserNotFoundException;
import com.dialog.exception.UserRoleAccessDeniedException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;

// 모든 REST API 예외를 한 곳에서 처리하는 글로벌 예외 핸들러 클래스
@Slf4j
@RestControllerAdvice // 모든 REST 컨트롤러의 예외를 공통 처리
public class GlobalExceptionHandler {

	@ExceptionHandler(GoogleOAuthException.class)
    public ResponseEntity<Map<String, String>> handleGoogleOAuthException(GoogleOAuthException e) {
        log.warn(" Google OAuth Error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("errorCode", "GOOGLE_REAUTH_REQUIRED", "message", e.getMessage()));
    }

	// 2. 리소스 찾기 실패 (404)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFound(ResourceNotFoundException e) {
        log.warn(" Resource Not Found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Not Found", "message", e.getMessage()));
    }
    
    // 3. 접근 거부 (403)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException e) {
         log.warn(" Access Denied: {}", e.getMessage());
         return ResponseEntity.status(HttpStatus.FORBIDDEN)
                 .body(Map.of("error", "Forbidden", "message", e.getMessage()));
    }
    // 비활성화시 발생되는 예외처리.
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, String>> handleDisabledException(DisabledException e) {
        log.warn("비활성화된 계정 접근 시도: {}", e.getMessage());
        Map<String, String> responseBody = Map.of(
            "status", "401",
            "error", "Unauthorized",
            "message", e.getMessage() // "계정이 비활성화되었습니다. 관리자에게 문의하세요."
        );        
        return new ResponseEntity<>(responseBody, HttpStatus.UNAUTHORIZED);
    }
    
    // 4. 잘못된 요청 (400) - 기존 로직 유지하되 더 깔끔하게
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException e) {
        log.warn(" Bad Request: {}", e.getMessage());
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
    
    // 5. 그 외 서버 에러 (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        log.error(" Internal Server Error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal Server Error", "message", "서버 내부 오류가 발생했습니다."));
    }
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String,String>> handleUserNotFound(UserNotFoundException e){
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "사용자 없음", "message", e.getMessage()));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String,String>> handleUserExists(UserAlreadyExistsException e){
        return ResponseEntity.badRequest()
            .body(Map.of("error", "이미 존재하는 사용자", "message", e.getMessage()));
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<Map<String,String>> handleInvalidPassword(InvalidPasswordException e){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", "비밀번호 오류", "message", e.getMessage()));
    }

    @ExceptionHandler(InactiveUserException.class)
    public ResponseEntity<Map<String,String>> handleInactiveUser(InactiveUserException e){
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of("error", "비활성 사용자", "message", e.getMessage()));
    }

    @ExceptionHandler(UserRoleAccessDeniedException.class)
    public ResponseEntity<Map<String,String>> handleRoleAccessDenied(UserRoleAccessDeniedException e){
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of("error", "권한 없음", "message", e.getMessage()));
    }
    
    @ExceptionHandler(SocialUserSaveException.class)
    public ResponseEntity<Map<String,String>> handleSocialUserSave(SocialUserSaveException e){
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of("error", "소셜 사용자 저장 실패", "message", e.getMessage()));
    }
    
    @ExceptionHandler(RefreshTokenException.class)
    public ResponseEntity<Map<String,String>> handleRefreshTokenException(RefreshTokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                 .body(Map.of("error", "리프레시 토큰 오류", "message", e.getMessage()));
    }
    
    @ExceptionHandler(SocialUserInfoException.class)
    public ResponseEntity<Map<String, String>> handleSocialUserInfoException(SocialUserInfoException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
             .body(Map.of("error", "소셜 사용자 정보 오류", "message", e.getMessage()));
    }
    
    @ExceptionHandler(InvalidJwtTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidJwtTokenException(InvalidJwtTokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
             .body(Map.of("error", "유효하지 않은 토큰", "message", e.getMessage()));
    }
    
    @ExceptionHandler(OAuthUserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleOAuthUserNotFound(OAuthUserNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "OAuth 사용자 없음", "message", e.getMessage()));
    }
    
    @ExceptionHandler(TermsNotAcceptedException.class)
    public ResponseEntity<Map<String, String>> handleTermsNotAccepted(TermsNotAcceptedException e) {
        log.warn("Terms Not Accepted: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(Map.of("error", "약관 미동의", "message", e.getMessage()));
    }
    
    @ExceptionHandler(ChatbotApiException.class)
    public ResponseEntity<Map<String, String>> handleChatbotApiException(ChatbotApiException e) {
        log.error("챗봇 API 호출 실패: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Chatbot API Error","message", e.getMessage()));
    }
}