package com.example.kwizi.exception;

public class UserOfflineException extends RuntimeException {
    public UserOfflineException(String message) {
        super(message);
    }
}