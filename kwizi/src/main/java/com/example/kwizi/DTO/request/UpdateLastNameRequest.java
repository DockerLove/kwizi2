package com.example.kwizi.DTO.request;

import jakarta.validation.constraints.Size;

public class UpdateLastNameRequest {

    @Size(min = 2,max = 30,message = "Фамилия должна быть от 2 до 30 символов")
    private String lastName;

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
