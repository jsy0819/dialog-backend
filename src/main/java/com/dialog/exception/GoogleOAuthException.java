package com.dialog.exception;

public class GoogleOAuthException extends RuntimeException {
	
	// 1. 구글 OAuth 인증 실패 예외 (401)
	public GoogleOAuthException(String message) {
        super(message);
    }
	
	// 2. 리소스를 찾을 수 없음 예외 (404) - Optional
	public static class ResourceNotFoundException extends RuntimeException {
	    public ResourceNotFoundException(String message) {
	        super(message);
	    }
	}
	
	// 3. 권한 없음 예외 (403) - Optional (기존 IllegalAccessException 대체용)
	public static class AccessDeniedException extends RuntimeException {
	    public AccessDeniedException(String message) {
	        super(message);
	    }
	}
}
