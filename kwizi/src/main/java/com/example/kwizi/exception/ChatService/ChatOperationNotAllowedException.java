package com.example.kwizi.exception.ChatService;

public class ChatOperationNotAllowedException extends ChatException {
    public ChatOperationNotAllowedException(String message) {
        super(message);
    }
}