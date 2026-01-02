package com.example.kwizi.DTO.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на обновление имени пользователя (username)")
public class UpdateUsernameRequest {

    @Schema(
            description = "Новое имя пользователя (логин)",
            example = "new_username",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 1,
            maxLength = 30
    )
    @NotBlank(message = "Username не должен быть пустым")
    @Size(min = 1, max = 30, message = "Username должен быть от 1 до 30 символов")
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}