package com.example.kwizi.exception.ChatService;

public class NotGroupChatException extends RuntimeException {
    public NotGroupChatException(String message) {
        super(message);
    }
}