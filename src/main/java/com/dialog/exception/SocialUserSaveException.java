package com.dialog.exception;

public class SocialUserSaveException extends RuntimeException {
    public SocialUserSaveException(String message) {
        super(message);
    }
    public SocialUserSaveException(String message, Throwable cause) {
        super(message, cause);
    }
}
