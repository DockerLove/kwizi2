package com.example.kwizi.DTO.internal;

import com.example.kwizi.enums.MessageType;
import com.example.kwizi.exception.MessageValidationException;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;

public class MessageEventDto {

    private MessageType type;
    private Long senderId;
    private Long recipientId;
    private Long chatId;
    private String text;
    private Instant timestamp;

    @JsonIgnore
    public void validate() {
        if (type == null) {
            throw new MessageValidationException("Тип сообщения обязателен");
        }

        if (text == null || text.trim().isEmpty()) {
            throw new MessageValidationException("Текст сообщения не может быть пустым");
        }

        if (senderId == null) {
            throw new MessageValidationException("ID отправителя обязателен");
        }

        switch (type) {
            case PRIVATE:
                if (recipientId == null) {
                    throw new MessageValidationException("ID получателя обязателен для приватных сообщений");
                }
                if (recipientId.equals(senderId)) {
                    throw new MessageValidationException("Нельзя отправить приватное сообщение самому себе");
                }
                break;

            case GROUP:
                if (chatId == null) {
                    throw new MessageValidationException("ID чата обязателен для групповых сообщений");
                }
                break;
        }
    }

    @JsonIgnore
    public String getTargetTopic() {
        return switch (this.type) {
            case PRIVATE -> "private-messages";
            case GROUP -> "group-messages";
        };
    }

    @JsonIgnore
    public boolean isPrivate() {
        return type == MessageType.PRIVATE;
    }

    @JsonIgnore
    public boolean isGroup() {
        return type == MessageType.GROUP;
    }

    @JsonIgnore
    public String getLogInfo() {
        if (isPrivate()) {
            return String.format("Приватное сообщение: %d -> %d", senderId, recipientId);
        } else {
            return String.format("Групповое сообщение: %d -> чат %d", senderId, chatId);
        }
    }

    public MessageEventDto() {
    }

    public MessageEventDto(MessageType type, Long senderId, Long recipientId, Long chatId, String text, Instant timestamp) {
        this.type = type;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.chatId = chatId;
        this.text = text;
        this.timestamp = timestamp;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}