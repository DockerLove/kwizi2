package com.example.kwizi.DTO.response;

import com.example.kwizi.enums.ChatType;

import java.time.OffsetDateTime;

public class ChatPreviewResponse {
    private Long id;
    private ChatType chatType;
    private String displayName;
    private String lastMessagePreview;
    private OffsetDateTime lastActivityAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setChatType(ChatType chatType) {
        this.chatType = chatType;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setLastMessagePreview(String lastMessagePreview) {
        this.lastMessagePreview = lastMessagePreview;
    }

    public void setLastActivityAt(OffsetDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }
}