package com.dialog;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.HttpStatus;

// 모든 REST API 예외를 한 곳에서 처리하는 글로벌 예외 핸들러 클래스
// 컨트롤러(@RestController)에서 IllegalArgumentException, IllegalStateException이 발생할 때 JSON 응답으로 처리

@RestControllerAdvice // 모든 REST 컨트롤러의 예외를 공통 처리
public class GlobalExceptionHandler {

 
    // IllegalArgumentException, IllegalStateException 예외 발생 시
    //  - 서버에서 예외 발생 시 여기서 잡아서 클라이언트에 JSON 형태의 에러 응답 반환
    //  @param ex 런타임 예외 객체
    //  @return { "message": 예외메시지 } 형태의 JSON과 400(Bad Request) 코드 반환
     
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<?> handleCustomException(RuntimeException ex) {
    	
    	String message = ex.getMessage();
    	
    	// 1. [새 로직] Google 토큰 오류(invalid_grant 또는 토큰 없음)인지 먼저 확인
        if (message != null && (message.contains("invalid_grant") || message.contains("연동 토큰이 없습니다"))) {
            
            // 401 Unauthorized 응답 반환
            Map<String, String> errorResponse = Map.of(
                "errorCode", "GOOGLE_REAUTH_REQUIRED",
                "message", "Google 연동이 만료되었거나 없습니다. 재연동이 필요합니다."
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
        }
    	
        // 1. 에러 메시지만 담는 Map 객체 생성
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());

        // 2. 400 Bad Request와 함께 JSON 응답 반환
        return ResponseEntity.badRequest().body(error);
    }
    
    /**
     * [새 로직] NullPointerException 등 위에서 잡지 못한
     * 나머지 모든 RuntimeException을 처리합니다. (500 에러)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleGenericRuntimeException(RuntimeException ex) {
        
        // (중요) 서버 로그에 스택 트레이스를 남겨야 디버깅이 가능합니다.
        // log.error("서버 내부 오류 발생", ex); 
        
        return new ResponseEntity<>(
            Map.of("error", "서버 내부 오류가 발생했습니다: " + ex.getMessage()), 
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}