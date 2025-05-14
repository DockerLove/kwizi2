package com.example.authentication_service.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegistrationRequestDto {
    @NotBlank(message = "Это поле не должен быть пустым")
    @Size(min = 3, max = 20, message = "Введите логин от 3 до 20 символов")
    private String username;

    @NotBlank(message = "Это поле не должен быть пустым")
    @Size(min = 8, message = "Введите пароль от 8 символов")
    private String password;
    @NotBlank(message = "Это поле не должен быть пустым")
    @Email(message = "Email должен удовлетроворять требованиям")
    private String email;
    @NotBlank(message = "Это поле не должен быть пустым")
    private String firstName;
    @NotBlank(message = "Это поле не должен быть пустым")
    private String lastName;

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