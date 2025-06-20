package com.example.kwizi.DTO.internal;
public class GroupMessageDto {
    private Long chatId;
    private String text;

    // Геттеры и сеттеры
    public Long getChatId() { return chatId; }
    public void setChatId(Long chatId) { this.chatId = chatId; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}