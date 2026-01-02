package com.example.kwizi.DTO.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на создание приватного чата")
public class CreatePrivateChatRequest {

    @Schema(
            description = "Имя пользователя получателя",
            example = "anna_smith",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Username получателя не может быть пустым")
    private String recipientUsername;

    public String getRecipientUsername() {
        return recipientUsername;
    }

    public void setRecipientUsername(String recipientUsername) {
        this.recipientUsername = recipientUsername;
    }
}