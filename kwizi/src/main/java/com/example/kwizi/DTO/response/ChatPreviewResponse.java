package com.example.kwizi.DTO.response;

import com.example.kwizi.enums.ChatType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Предварительный просмотр чата")
public class ChatPreviewResponse {

    @Schema(
            description = "ID чата",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long id;

    @Schema(
            description = "Тип чата",
            example = "PRIVATE",
            requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"PRIVATE", "GROUP"}
    )
    private ChatType chatType;

    @Schema(
            description = "Отображаемое имя чата",
            example = "Иван Иванов",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String displayName;

    @Schema(
            description = "Предпросмотр последнего сообщения",
            example = "Привет! Как дела?",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String lastMessagePreview;

    @Schema(
            description = "Время последней активности в чате",
            example = "2024-01-15T10:30:00+03:00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private OffsetDateTime lastActivityAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setChatType(ChatType chatType) {
        this.chatType = chatType;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setLastMessagePreview(String lastMessagePreview) {
        this.lastMessagePreview = lastMessagePreview;
    }

    public void setLastActivityAt(OffsetDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }
}