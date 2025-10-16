package com.example.kwizi.exception.ChatService;

public class DuplicateChatMemberException extends ChatException {
    public DuplicateChatMemberException(String message) {
        super(message);
    }
}
