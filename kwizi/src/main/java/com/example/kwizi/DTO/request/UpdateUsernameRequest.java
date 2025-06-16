package com.example.kwizi.DTO.request;

import jakarta.validation.constraints.Size;

public class UpdateUsernameRequest {


    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
