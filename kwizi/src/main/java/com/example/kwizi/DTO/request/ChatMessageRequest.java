package com.example.kwizi.DTO.request;

import com.example.kwizi.enums.MessageType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PrivateMessageRequest.class, name = "PRIVATE"),
        @JsonSubTypes.Type(value = GroupMessageRequest.class, name = "GROUP")
})
@Schema(
        description = "Базовый абстрактный запрос на отправку сообщения",
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "PRIVATE", schema = PrivateMessageRequest.class),
                @DiscriminatorMapping(value = "GROUP", schema = GroupMessageRequest.class)
        },
        oneOf = {PrivateMessageRequest.class, GroupMessageRequest.class}
)
public abstract class ChatMessageRequest {

    @Schema(
            description = "Тип сообщения",
            example = "PRIVATE",
            requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"PRIVATE", "GROUP"}
    )
    private MessageType type;

    @Schema(
            description = "Текст сообщения",
            example = "Привет! Как дела?",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = 2000
    )
    @NotBlank(message = "Текст сообщения не может быть пустым")
    @Size(max = 2000, message = "Сообщение не может превышать 2000 символов")
    private String text;

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}