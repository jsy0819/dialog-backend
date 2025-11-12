package com.dialog.exception;

// GoogleOAuthException을 상속받아, 재인증이 필요함을 명확히 하는 커스텀 예외
// GlobalExceptionHandler에서 이 예외를 401 Unauthorized + 특정 메시지로 매핑
public class GoogleReauthRequiredException extends GoogleOAuthException {
    
	// 사용자에게 표시할 오류 메시지 (재연동 필요 내용 포함)
    public GoogleReauthRequiredException(String message) {
        super(message);
    }

    //예외 메시지와 발생 원인(cause)을 포함하는 생성자
    //이 예외를 유발한 원본 Throwable (e.g., WebClientException)    
    public GoogleReauthRequiredException(String message, Throwable cause) {
        super(message, cause);
    }
}