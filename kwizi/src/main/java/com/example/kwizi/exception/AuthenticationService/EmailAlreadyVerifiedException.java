package com.example.kwizi.exception.AuthenticationService;

public class EmailAlreadyVerifiedException extends RuntimeException {
    public EmailAlreadyVerifiedException(String message) { super(message); }
}
