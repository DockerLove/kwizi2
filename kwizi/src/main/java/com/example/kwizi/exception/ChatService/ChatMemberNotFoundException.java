package com.example.kwizi.exception.ChatService;

public class ChatMemberNotFoundException extends ChatException {
    public ChatMemberNotFoundException(String message) {
        super(message);
    }
}