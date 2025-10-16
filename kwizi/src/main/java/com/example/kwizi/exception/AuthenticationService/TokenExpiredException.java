package com.example.kwizi.exception.AuthenticationService;

public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) { super(message); }
}