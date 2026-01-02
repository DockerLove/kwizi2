package com.example.kwizi.DTO.request;

import jakarta.validation.constraints.NotNull;

public class AddChatMemberRequestDto {

    private Long chatId;

    @NotNull(message = "Поле userId не должно быть пустое")
    private Long userId;

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