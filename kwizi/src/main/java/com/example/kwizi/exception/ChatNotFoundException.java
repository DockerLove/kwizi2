package com.example.kwizi.exception;

public class ChatNotFoundException extends RuntimeException{
    public ChatNotFoundException(String message) {
        super(message);
    }
}
