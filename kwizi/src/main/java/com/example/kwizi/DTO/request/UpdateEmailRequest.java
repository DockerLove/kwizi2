package com.example.kwizi.DTO.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UpdateEmailRequest {

    @NotBlank(message = "Email не должен быть пустой")
    @Email(message = "Email должен удовлетроворять требованиям")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
