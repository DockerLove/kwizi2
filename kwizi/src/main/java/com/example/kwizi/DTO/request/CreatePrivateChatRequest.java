package com.example.kwizi.DTO.request;

import jakarta.validation.constraints.NotBlank;

public class CreatePrivateChatRequest {
    @NotBlank(message = "Username получателя не может быть пустым")
    private String recipientUsername;

    public String getRecipientUsername() {
        return recipientUsername;
    }

    public void setRecipientUsername(String recipientUsername) {
        this.recipientUsername = recipientUsername;
    }
}
