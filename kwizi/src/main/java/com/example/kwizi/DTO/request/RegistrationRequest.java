package com.example.kwizi.DTO.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegistrationRequest {
    @NotBlank(message = "Это поле не должен быть пустым")
    @Size(min = 3, max = 20, message = "Введите логин от 3 до 20 символов")
    private String username;

    @NotBlank(message = "Это поле не должен быть пустым")
    @Size(min = 8, message = "Введите пароль от 8 символов")
    private String password;

    @NotBlank(message = "Это поле не должен быть пустым")
    @Email(message = "Email должен удовлетроворять требованиям")
    private String email;

    @NotBlank(message = "Имя не должно быть пустым")
    @Size(min = 2,max = 30, message = "Введите имя от 2 до 30 символов")
    private String firstName;

    @NotBlank(message = "Фамилия не должна быть пустой")
    @Size(min = 2,max = 30, message = "Введите фамилию от 2 до 30 символов")
    private String lastName;

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