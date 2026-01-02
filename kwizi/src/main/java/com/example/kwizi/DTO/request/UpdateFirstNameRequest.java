package com.example.kwizi.DTO.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на обновление имени пользователя")
public class UpdateFirstNameRequest {

    @Schema(
            description = "Новое имя пользователя",
            example = "Александр",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 2,
            maxLength = 30
    )
    @Size(min = 2, max = 30, message = "Имя должно быть от 2 до 30 символов")
    @NotBlank(message = "Имя не должно быть пустым")
    private String firstName;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
}