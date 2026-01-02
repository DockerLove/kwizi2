package com.example.kwizi.DTO.request;

import com.example.kwizi.enums.MessageType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PrivateMessageRequest.class, name = "PRIVATE"),
        @JsonSubTypes.Type(value = GroupMessageRequest.class, name = "GROUP")
})
public abstract class ChatMessageRequest {
    private MessageType type;
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