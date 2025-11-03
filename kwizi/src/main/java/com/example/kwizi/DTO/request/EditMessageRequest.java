package com.example.kwizi.DTO.request;

import jakarta.validation.constraints.NotBlank;

public class EditMessageRequest {
    @NotBlank
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
