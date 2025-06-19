package com.example.kwizi.DTO.internal;
public class PrivateChatMessageDto {
    private String recipientUsername;
    private MessageDto messageDto;

    // Геттеры и сеттеры
    public String getRecipientUsername() {
        return recipientUsername;
    }

    public void setRecipientUsername(String recipientUsername) {
        this.recipientUsername = recipientUsername;
    }

    public MessageDto getMessageDto() {
        return messageDto;
    }

    public void setMessageDto(MessageDto messageDto) {
        this.messageDto = messageDto;
    }
}