package com.example.kwizi.DTO.request;

public class AddChatMemberRequestDto {
    private Long chatId;
    private Long userId;

    // Getters and setters
    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}