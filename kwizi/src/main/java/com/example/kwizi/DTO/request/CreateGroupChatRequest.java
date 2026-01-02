package com.example.kwizi.DTO.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Запрос на создание группового чата")
public class CreateGroupChatRequest {

    @Schema(
            description = "Название группового чата",
            example = "Рабочая группа проекта",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 1,
            maxLength = 100
    )
    @NotBlank(message = "Название группы не может быть пустым")
    @Size(min = 1, max = 100, message = "Название группы должно быть от 1 до 100 символов")
    private String groupName;

    @Schema(
            description = "Список ID пользователей для добавления в группу при создании",
            example = "[2, 3, 5]",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @ArraySchema(
            schema = @Schema(description = "ID пользователя", example = "1"),
            minItems = 0,
            maxItems = 50
    )
    private List<Long> initialMemberIds;

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<Long> getInitialMemberIds() {
        return initialMemberIds;
    }

    public void setInitialMemberIds(List<Long> initialMemberIds) {
        this.initialMemberIds = initialMemberIds;
    }
}