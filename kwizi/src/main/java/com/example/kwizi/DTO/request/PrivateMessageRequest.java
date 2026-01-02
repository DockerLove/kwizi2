package com.example.kwizi.DTO.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Запрос на отправку приватного сообщения")
public class PrivateMessageRequest extends ChatMessageRequest {

    @Schema(
            description = "ID получателя сообщения",
            example = "3",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long recipientId;

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }
}