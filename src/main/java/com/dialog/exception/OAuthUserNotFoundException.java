package com.dialog.exception;

public class OAuthUserNotFoundException extends RuntimeException {
    public OAuthUserNotFoundException(String message) { super(message); }
}
