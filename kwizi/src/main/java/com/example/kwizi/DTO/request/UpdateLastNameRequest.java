package com.example.kwizi.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateLastNameRequest {

    @Size(min = 2,max = 30,message = "Фамилия должна быть от 2 до 30 символов")
    @NotBlank(message = "Фамилия не должна быть пустой")
    private String lastName;

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
