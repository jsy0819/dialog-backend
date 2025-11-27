package com.dialog.exception;

public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException(String msg) { super(msg); }
}
