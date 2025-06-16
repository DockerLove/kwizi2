package com.example.kwizi.DTO;

import jakarta.validation.constraints.Size;

public class UpdateLastNameRequest {

    @Size(max = 30,message = "Фамилия должна быть не более 30 символов")
    private String lastName;

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
