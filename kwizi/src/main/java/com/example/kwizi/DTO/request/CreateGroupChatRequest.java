package com.example.kwizi.DTO.request;

import java.util.List;

public class CreateGroupChatRequest {
    private String groupName;
    private Long creatorId;
    private List<Long> initialMemberIds;

    // Getters and setters
    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public List<Long> getInitialMemberIds() {
        return initialMemberIds;
    }

    public void setInitialMemberIds(List<Long> initialMemberIds) {
        this.initialMemberIds = initialMemberIds;
    }
}