package com.example.kwizi.exception.AuthenticationService;

public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException(String message) { super(message); }
}