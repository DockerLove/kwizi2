package com.example.kwizi.DTO.request;

import jakarta.validation.constraints.Size;

public class UpdateBioRequest {

    @Size(max =500, message="Bio должно быть не более 500 символов")
    private String bio;

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}
