package com.example.kwizi.service;

import com.example.kwizi.DTO.internal.MessageDto;
import com.example.kwizi.DTO.request.GroupMessageRequest;
import com.example.kwizi.DTO.request.PrivateMessageRequest;
import com.example.kwizi.DTO.response.WebSocketResponse;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.model.Message;
import com.example.kwizi.websocket.UniversalChatHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
@Service
public class KafkaMessageConsumer {
    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageConsumer.class);
    private final ObjectMapper objectMapper;
    private final ChatMessageService chatMessageService;
    private final UniversalChatHandler chatHandler;

    @Autowired
    public KafkaMessageConsumer(ObjectMapper objectMapper,
                                ChatMessageService chatMessageService,
                                UniversalChatHandler chatHandler) {
        this.objectMapper = objectMapper;
        this.chatMessageService = chatMessageService;
        this.chatHandler = chatHandler;
    }

    @KafkaListener(topics = "private-messages", groupId = "websocket-group")
    public void consumePrivateMessage(String kafkaMessage) {
        try {
            JsonNode message = objectMapper.readTree(kafkaMessage);
            Long senderId = message.get("senderId").asLong();
            Long recipientId = message.get("recipientId").asLong();
            String text = message.get("text").asText();

            // 1. Сохраняем в БД
            MessageDto messageDto = new MessageDto();
            messageDto.setText(text);
            Message savedMessage = chatMessageService.sendPrivateMessage(messageDto, senderId, recipientId);

            // 2. Отправляем получателю через activeSessions
            sendToSession(
                    recipientId,
                    formatResponse("MESSAGE", convertToDto(savedMessage))
            );

            // 3. Отправляем подтверждение отправителю
            sendToSession(
                    senderId,
                    formatResponse("DELIVERY_CONFIRMATION",
                            Map.of(
                                    "messageId", savedMessage.getId(),
                                    "status", "DELIVERED"
                            )
                    )
            );

            logger.info("Private message processed: {} → {}", senderId, recipientId);
        } catch (UserNotFoundException e) {
            logger.warn("User not found: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to process private message: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "group-messages", groupId = "websocket-group")
    public void consumeGroupMessage(String kafkaMessage) {
        try {
            JsonNode message = objectMapper.readTree(kafkaMessage);
            Long senderId = message.get("senderId").asLong();
            Long chatId = message.get("chatId").asLong();
            String text = message.get("text").asText();

            // 1. Сохраняем в БД
            MessageDto messageDto = new MessageDto();
            messageDto.setChatId(chatId);
            messageDto.setText(text);
            Message savedMessage = chatMessageService.sendMessage(messageDto, senderId);

            // 2. Отправляем всем участникам чата
            List<Long> members = chatMessageService.getChatMembers(chatId);
            for (Long memberId : members) {
                if (!memberId.equals(senderId)) {
                    sendToSession(
                            memberId,
                            formatResponse("GROUP_MESSAGE",
                                    Map.of(
                                            "chatId", chatId,
                                            "message", convertToDto(savedMessage)
                                    )
                            )
                    );
                }
            }

            logger.info("Group message processed in chat {} by user {}", chatId, senderId);
        } catch (Exception e) {
            logger.error("Failed to process group message: {}", e.getMessage());
        }
    }

    private void sendToSession(Long userId, String message) {
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

    private String formatResponse(String type, Object data) {
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

    private MessageDto convertToDto(Message message) {
        MessageDto dto = new MessageDto();
        dto.setId(message.getId());
        dto.setText(message.getText());
        dto.setCreatedAt(message.getCreatedAt().toLocalDateTime());

        if (message.getSender() != null) {
            dto.setSenderId(message.getSender().getId());
        }

        if (message.getChat() != null) {
            dto.setChatId(message.getChat().getId());
        }

        return dto;
    }
}