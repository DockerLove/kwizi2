package com.example.kwizi.exception.MessageService;

public class MessageEditTimeExpiredException extends RuntimeException {
    public MessageEditTimeExpiredException(String message) {
        super(message);
    }
}