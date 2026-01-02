package com.example.kwizi.DTO.internal;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "DTO для представления сообщения")
public class MessageDto {

    @Schema(
            description = "Уникальный идентификатор сообщения",
            example = "12345",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long id;

    @Schema(
            description = "ID чата, в котором отправлено сообщение",
            example = "5",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long chatId;

    @Schema(
            description = "ID отправителя сообщения",
            example = "3",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long senderId;

    @Schema(
            description = "Текст сообщения",
            example = "Привет! Как дела?",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String text;

    @Schema(
            description = "Время создания сообщения",
            example = "2024-01-15T10:30:00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}