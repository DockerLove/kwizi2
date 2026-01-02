package com.example.kwizi.DTO.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на обновление фамилии пользователя")
public class UpdateLastNameRequest {

    @Schema(
            description = "Новая фамилия пользователя",
            example = "Иванов",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 2,
            maxLength = 30
    )
    @Size(min = 2, max = 30, message = "Фамилия должна быть от 2 до 30 символов")
    @NotBlank(message = "Фамилия не должна быть пустой")
    private String lastName;

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}