package com.example.kwizi.service.consumer;

import com.example.kwizi.DTO.internal.MessageDto;
import com.example.kwizi.model.Message;
import com.example.kwizi.service.ChatMessageService;
import com.example.kwizi.websocket.UniversalChatHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;

public abstract class BaseMessageConsumer {
    private static final Logger logger = LoggerFactory.getLogger(BaseMessageConsumer.class);

    protected final ObjectMapper objectMapper;
    protected final ChatMessageService chatMessageService;
    protected final UniversalChatHandler chatHandler;

    public BaseMessageConsumer(ObjectMapper objectMapper,
                               ChatMessageService chatMessageService,
                               UniversalChatHandler chatHandler) {
        this.objectMapper = objectMapper;
        this.chatMessageService = chatMessageService;
        this.chatHandler = chatHandler;
    }

    protected void sendToSession(Long userId, String message) {
        WebSocketSession session = chatHandler.getActiveSessions().get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                logger.warn("Failed to send to user {}: {}", userId, e.getMessage());
            }
        } else {
            logger.debug("User {} is offline or session closed", userId);
        }
    }
    protected String formatResponse(String type, Object data) {
        try {
            return objectMapper.writeValueAsString(
                    Map.of(
                            "type", type,
                            "data", data,
                            "timestamp", Instant.now().toString()
                    )
            );
        } catch (JsonProcessingException e) {
            logger.error("Failed to format response: {}", e.getMessage());
            return "{\"type\":\"ERROR\",\"message\":\"Response formatting failed\"}";
        }
    }
    protected MessageDto convertToDto(Message message) {
        MessageDto dto = new MessageDto();
        dto.setId(message.getId());
        dto.setText(message.getText());
        dto.setCreatedAt(OffsetDateTime.from(message.getCreatedAt().toOffsetTime()));

        if (message.getSender() != null) {
            dto.setSenderId(message.getSender().getId());
        }

        if (message.getChat() != null) {
            dto.setChatId(message.getChat().getId());
        }

        return dto;
    }

    protected void sendErrorToSender(Long senderId, String errorCode, String errorMessage) {
        try {
            sendToSession(
                    senderId,
                    formatResponse("DELIVERY_FAILED",
                            Map.of(
                                    "errorCode", errorCode,
                                    "errorMessage", errorMessage
                            )
                    )
            );
        } catch (Exception e) {
            logger.error("Failed to send error notification to sender {}: {}", senderId, e.getMessage());
        }
    }
}