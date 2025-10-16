package com.example.kwizi.exception.AuthenticationService;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) { super(message); }
}