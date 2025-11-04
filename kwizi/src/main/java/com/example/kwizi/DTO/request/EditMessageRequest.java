package com.example.kwizi.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EditMessageRequest {
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
