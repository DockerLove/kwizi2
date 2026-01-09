package com.example.kwizi.DTO.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Запрос на добавление участника в чат")
public class AddChatMemberRequestDto {
    private Long chatId;

    @Schema(
            description = "ID пользователя для добавления в чат",
            example = "3",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Поле userId не должно быть пустое")
    private Long userId;

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}