package com.example.user_service.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateUserDto {

    @NotBlank(message = "Это поле не должно быть пустым")
    @Email(message = "Email должен соответствовать формату")
    private String email;
    @NotBlank(message = "Это поле не должно быть пустым")
    @Size(min = 2,max = 30, message = "Введите имя от 2 до 30 символов")
    private String firstName;
    @NotBlank(message = "Это поле не должно быть пустым")
    @Size(min = 2,max = 30, message = "Введите фамилию от 2 до 30 символов")
    private String lastName;

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
