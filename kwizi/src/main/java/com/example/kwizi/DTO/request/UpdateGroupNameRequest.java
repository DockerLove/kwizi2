package com.example.kwizi.DTO.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на изменение названия группового чата")
public class UpdateGroupNameRequest {

    @Schema(
            description = "Новое название группы",
            example = "Новое название команды",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 1,
            maxLength = 100
    )
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