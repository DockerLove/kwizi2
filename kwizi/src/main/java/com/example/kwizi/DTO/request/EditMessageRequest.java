package com.example.kwizi.DTO.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на редактирование сообщения")
public class EditMessageRequest {

    @Schema(
            description = "Новый текст сообщения",
            example = "Обновленный текст сообщения",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = 2000
    )
    @NotBlank(message = "Текст сообщения не может быть пустым")
    @Size(max = 2000, message = "Сообщение не может превышать 2000 символов")
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}