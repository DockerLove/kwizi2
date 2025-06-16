package com.example.kwizi.DTO;

import jakarta.validation.constraints.Size;

public class UpdateFirstNameRequest {

    @Size(max = 20, message = "Имя должно быть не больше 20 символов")
    private String firstName;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
}
