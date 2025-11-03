package com.example.kwizi.exception.MessageService;

public class MessageAccessDeniedException extends RuntimeException {
    public MessageAccessDeniedException(String message) {
        super(message);
    }
}