package com.example.kwizi.DTO.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на обновление email пользователя")
public class UpdateEmailRequest {

    @Schema(
            description = "Новый email адрес пользователя",
            example = "new.email@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Email не должен быть пустой")
    @Email(message = "Email должен удовлетворять требованиям")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}