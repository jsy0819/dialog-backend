package com.dialog;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 모든 REST API 예외를 한 곳에서 처리하는 글로벌 예외 핸들러 클래스
 * - 컨트롤러(@RestController)에서 IllegalArgumentException, IllegalStateException이 발생할 때 JSON 응답으로 처리
 */
@RestControllerAdvice // 모든 REST 컨트롤러의 예외를 공통 처리
public class GlobalExceptionHandler {

    /**
     * IllegalArgumentException, IllegalStateException 예외 발생 시
     * - 서버에서 예외 발생 시 여기서 잡아서 클라이언트에 JSON 형태의 에러 응답 반환
     * @param ex 런타임 예외 객체
     * @return { "message": 예외메시지 } 형태의 JSON과 400(Bad Request) 코드 반환
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<?> handleCustomException(RuntimeException ex) {
        // 1. 에러 메시지만 담는 Map 객체 생성
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());

        // 2. 400 Bad Request와 함께 JSON 응답 반환
        return ResponseEntity.badRequest().body(error);
    }
}