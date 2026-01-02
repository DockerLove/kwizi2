package com.example.kwizi.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CreateGroupChatRequest {
    @NotBlank(message = "Название группы не может быть пустым")
    @Size(min = 1, max = 100, message = "Название группы должно быть от 1 до 100 символов")
    private String groupName;
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