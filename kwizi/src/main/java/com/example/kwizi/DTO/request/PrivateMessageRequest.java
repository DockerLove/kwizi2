package com.example.kwizi.DTO.request;
public class PrivateMessageRequest extends ChatMessageRequest {
    private Long recipientId;

    // Геттеры и сеттеры

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }
}