package com.example.kwizi.DTO.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос аутентификации пользователя")
public class AuthenticationRequest {

    @Schema(
            description = "Имя пользователя или email",
            example = "user@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Поле username не должно быть пустое")
    private String username;

    @Schema(
            description = "Пароль пользователя",
            example = "MySecurePassword123!",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 6
    )
    @NotBlank(message = "Поле password не должно быть пустое")
    private String password;

    public AuthenticationRequest() {
    }

    public AuthenticationRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}