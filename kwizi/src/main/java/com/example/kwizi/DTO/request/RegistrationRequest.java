package com.example.kwizi.DTO.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос регистрации нового пользователя")
public class RegistrationRequest {

    @Schema(
            description = "Имя пользователя (логин)",
            example = "john_doe",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 3,
            maxLength = 20
    )
    @NotBlank(message = "Это поле не должен быть пустым")
    @Size(min = 3, max = 20, message = "Введите логин от 3 до 20 символов")
    private String username;

    @Schema(
            description = "Пароль пользователя",
            example = "SecurePassword123!",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 8
    )
    @NotBlank(message = "Это поле не должен быть пустым")
    @Size(min = 8, message = "Введите пароль от 8 символов")
    private String password;

    @Schema(
            description = "Email пользователя",
            example = "john.doe@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Это поле не должен быть пустым")
    @Email(message = "Email должен удовлетроворять требованиям")
    private String email;

    @Schema(
            description = "Имя пользователя",
            example = "John",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 2,
            maxLength = 30
    )
    @NotBlank(message = "Имя не должно быть пустым")
    @Size(min = 2, max = 30, message = "Введите имя от 2 до 30 символов")
    private String firstName;

    @Schema(
            description = "Фамилия пользователя",
            example = "Doe",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 2,
            maxLength = 30
    )
    @NotBlank(message = "Фамилия не должна быть пустой")
    @Size(min = 2, max = 30, message = "Введите фамилию от 2 до 30 символов")
    private String lastName;

    @Schema(
            description = "Биография пользователя",
            example = "Программист, любитель путешествий",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String bio;

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}