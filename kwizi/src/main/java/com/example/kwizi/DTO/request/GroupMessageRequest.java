package com.example.kwizi.DTO.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Запрос на отправку сообщения в групповой чат")
public class GroupMessageRequest extends ChatMessageRequest {

    @Schema(
            description = "ID группового чата",
            example = "5",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long chatId;

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }
}