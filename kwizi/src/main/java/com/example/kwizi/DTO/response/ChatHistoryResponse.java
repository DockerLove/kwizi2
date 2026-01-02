package com.example.kwizi.DTO.response;

import com.example.kwizi.DTO.internal.MessageDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Ответ с историей сообщений чата")
public class ChatHistoryResponse extends MessageDto {

    @Schema(
            description = "Уникальный идентификатор сообщения",
            example = "123",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long messageId;

    @Schema(
            description = "Имя отправителя сообщения",
            example = "Иван Иванов",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String senderName;

    @Schema(
            description = "Время последнего обновления сообщения",
            example = "2024-01-15T10:35:00Z",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Instant updatedAt;

    @Schema(
            description = "Признак редактирования сообщения",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private boolean edited;

    public ChatHistoryResponse() {
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isEdited() {
        return edited;
    }

    public void setEdited(boolean edited) {
        this.edited = edited;
    }
}