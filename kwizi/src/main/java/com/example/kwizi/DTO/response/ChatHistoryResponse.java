package com.example.kwizi.DTO.response;

import com.example.kwizi.DTO.internal.MessageDto;

import java.time.Instant;

public class ChatHistoryResponse extends MessageDto{
    private Long messageId;
    private String senderName;
    private Instant updatedAt;
    private boolean edited;

    // Конструкторы
    public ChatHistoryResponse() {}

    // Геттеры и сеттеры
    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public boolean isEdited() { return edited; }
    public void setEdited(boolean edited) { this.edited = edited; }
}