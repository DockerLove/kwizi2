package com.example.kwizi.service.consumer;

import com.example.kwizi.DTO.internal.MessageDto;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.exception.UserOfflineException;
import com.example.kwizi.model.Message;
import com.example.kwizi.service.ChatMessageService;
import com.example.kwizi.websocket.UniversalChatHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;
@Service
public class PrivateMessageConsumer extends BaseMessageConsumer{
    private static final Logger logger = LoggerFactory.getLogger(PrivateMessageConsumer.class);

    public PrivateMessageConsumer(ObjectMapper objectMapper, ChatMessageService chatMessageService, UniversalChatHandler chatHandler) {
        super(objectMapper, chatMessageService, chatHandler);
    }

    @KafkaListener(topics = "private-messages", groupId = "websocket-group")
    public void consumePrivateMessage(String kafkaMessage) {
        Long senderId = null;
        try {
            JsonNode message = objectMapper.readTree(kafkaMessage);
            senderId = message.get("senderId").asLong();
            Long recipientId = message.get("recipientId").asLong();

            if (!chatHandler.isUserOnline(recipientId)) {
                throw new UserOfflineException("User " + recipientId + " offline");
            }
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
        } catch (UserOfflineException e) {
            logger.warn("Message delivery failed: {}", e.getMessage());
            throw e; // Пойдёт в DLQ после 3 ретраев
        } catch (UserNotFoundException e) {
            logger.warn("User not found: {}", e.getMessage());
            sendErrorToSender(senderId, "USER_NOT_FOUND", e.getMessage());
        }catch (JsonProcessingException e){
            logger.error("Invalid message format: {}", e.getMessage());
            sendErrorToSender(senderId, "INVALID_MESSAGE_FORMAT", "Message could not be parsed");
        } catch (Exception e) {
            logger.error("Failed to process private message: {}", e.getMessage());
            sendErrorToSender(senderId, "INTERNAL_ERROR", "Failed to deliver message");
        }
    }

}
