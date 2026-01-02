package com.example.kwizi.DTO.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на обновление биографии пользователя")
public class UpdateBioRequest {

    @Schema(
            description = "Новая биография пользователя",
            example = "Программист, любитель путешествий и фотографии",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = 500
    )
    @NotBlank(message = "Bio не должно быть пустым")
    @Size(max = 500, message = "Bio должно быть не более 500 символов")
    private String bio;

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}