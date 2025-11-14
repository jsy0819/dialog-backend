package com.dialog.exception;

public class GoogleOAuthException extends RuntimeException {
	
	// 1. 구글 OAuth 인증 실패 예외 (401)
	public GoogleOAuthException(String message) {
        super(message);
    }
	
    public GoogleOAuthException(String message, Throwable cause) {
        super(message, cause);
    }

}
