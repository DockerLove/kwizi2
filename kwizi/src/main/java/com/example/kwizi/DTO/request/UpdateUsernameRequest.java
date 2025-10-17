package com.example.kwizi.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateUsernameRequest {

    @NotBlank(message = "Username не должен быть пустым")
    @Size(min = 1, max = 30,message = "Username должен быть от 1 до 30 символов")
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
