package com.example.kwizi.DTO.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class CreateGroupChatRequest {
    @NotBlank(message = "Название группы не может быть пустым")
    private String groupName;
    private List<Long> initialMemberIds;

    // Getters and setters
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