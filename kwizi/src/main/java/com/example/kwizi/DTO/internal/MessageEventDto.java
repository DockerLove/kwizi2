package com.example.kwizi.DTO.internal;

import com.example.kwizi.enums.MessageType;
import com.example.kwizi.exception.MessageValidationException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "DTO события сообщения для Kafka и WebSocket")
public class MessageEventDto {

    @Schema(
            description = "Тип сообщения",
            example = "PRIVATE",
            requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"PRIVATE", "GROUP"}
    )
    private MessageType type;

    @Schema(
            description = "ID отправителя сообщения",
            example = "3",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long senderId;

    @Schema(
            description = "ID получателя (только для приватных сообщений)",
            example = "5",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private Long recipientId;

    @Schema(
            description = "ID чата (только для групповых сообщений)",
            example = "10",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private Long chatId;

    @Schema(
            description = "Текст сообщения",
            example = "Привет! Как дела?",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String text;

    @Schema(
            description = "Временная метка создания сообщения",
            example = "2024-01-15T10:30:00Z",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Instant timestamp;

    @Schema(hidden = true)
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

    @Schema(hidden = true)
    @JsonIgnore
    public String getTargetTopic() {
        return switch (this.type) {
            case PRIVATE -> "private-messages";
            case GROUP -> "group-messages";
        };
    }

    @Schema(hidden = true)
    @JsonIgnore
    public boolean isPrivate() {
        return type == MessageType.PRIVATE;
    }

    @Schema(hidden = true)
    @JsonIgnore
    public boolean isGroup() {
        return type == MessageType.GROUP;
    }

    @Schema(hidden = true)
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