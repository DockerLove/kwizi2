package com.example.kwizi.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateFirstNameRequest {
    @Size(min = 2,max = 30, message = "Имя должно быть от 2 до 30 символов")
    @NotBlank(message = "Имя не должно быть пустым")
    private String firstName;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
}
