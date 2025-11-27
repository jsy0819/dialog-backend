package com.dialog.exception;

public class SocialUserInfoException extends RuntimeException {
    public SocialUserInfoException(String message) {
        super(message);
    }
    public SocialUserInfoException(String message, Throwable cause) {
        super(message, cause);
    }
}