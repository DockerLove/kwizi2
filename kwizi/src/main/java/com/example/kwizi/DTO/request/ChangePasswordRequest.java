package com.example.kwizi.DTO.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос смены пароля пользователя")
public class ChangePasswordRequest {

    @Schema(
            description = "Текущий пароль пользователя",
            example = "OldPassword123!",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Поле oldPassword не должно быть пустое")
    private String oldPassword;

    @Schema(
            description = "Новый пароль пользователя",
            example = "NewSecurePassword456!",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 6
    )
    @NotBlank(message = "Поле newPassword не должно быть пустое")
    @Size(min = 6, message = "Новый пароль должен быть минимум 6 символов")
    private String newPassword;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}