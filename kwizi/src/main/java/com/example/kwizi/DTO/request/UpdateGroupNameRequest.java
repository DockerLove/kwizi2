package com.example.kwizi.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateGroupNameRequest {
    @NotBlank(message = "Название группы не может быть пустым")
    @Size(min = 1, max = 100, message = "Название группы должно быть от 1 до 100 символов")
    private String groupName;

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}