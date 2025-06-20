package com.example.kwizi.DTO.request;
public class GroupMessageRequest extends ChatMessageRequest {
    private Long chatId;

    // Геттеры и сеттеры


    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }
}