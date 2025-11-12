package com.dialog.exception;

public class UserRoleAccessDeniedException extends RuntimeException {
    public UserRoleAccessDeniedException(String msg) { super(msg); }
}
