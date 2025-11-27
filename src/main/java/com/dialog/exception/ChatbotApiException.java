package com.dialog.exception;

public class ChatbotApiException extends RuntimeException {
    public ChatbotApiException(String message) {
        super(message);
    }

    public ChatbotApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
