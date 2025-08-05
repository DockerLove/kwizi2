package com.example.kwizi.exception;
public class GroupMessageDeliveryException extends RuntimeException {
    public GroupMessageDeliveryException(String message) {
        super(message);
    }
}