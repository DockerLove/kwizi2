package com.example.kwizi.DTO.response;

import com.example.kwizi.enums.ChatType;

import java.time.OffsetDateTime;

public class ChatPreviewDto {
    private Long id;
    private ChatType chatType;
    private String displayName;  // Универсальное поле для отображения
    private String lastMessagePreview;
    private OffsetDateTime lastActivityAt;

    // Конструкторы
    public ChatPreviewDto() {}

    public ChatPreviewDto(Long id, ChatType chatType, String displayName,
                          String lastMessagePreview, OffsetDateTime lastActivityAt) {
        this.id = id;
        this.chatType = chatType;
        this.displayName = displayName;
        this.lastMessagePreview = lastMessagePreview;
        this.lastActivityAt = lastActivityAt;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ChatType getChatType() { return chatType; }
    public void setChatType(ChatType chatType) { this.chatType = chatType; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getLastMessagePreview() { return lastMessagePreview; }
    public void setLastMessagePreview(String lastMessagePreview) { this.lastMessagePreview = lastMessagePreview; }

    public OffsetDateTime getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(OffsetDateTime lastActivityAt) { this.lastActivityAt = lastActivityAt; }
}