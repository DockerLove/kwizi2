package com.example.kwizi.exception.ChatService;

public class InsufficientPermissionsException extends ChatException {
    public InsufficientPermissionsException(String message) {
        super(message);
    }
}