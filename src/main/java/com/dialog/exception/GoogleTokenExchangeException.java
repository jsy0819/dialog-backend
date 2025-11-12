package com.dialog.exception;

public class GoogleTokenExchangeException extends RuntimeException {
    public GoogleTokenExchangeException(String message) {
        super(message);
    }
    public GoogleTokenExchangeException(String message, Throwable cause) {
        super(message, cause);
    }
}
