package com.example.kwizi.service.consumer;

import com.example.kwizi.DTO.internal.MessageDto;
import com.example.kwizi.model.DeliveryState;
import com.example.kwizi.model.Message;
import com.example.kwizi.service.ChatMessageService;
import com.example.kwizi.websocket.UniversalChatHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.List;
import java.util.Map;

@Service
public class GroupMessageConsumer extends BaseMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(GroupMessageConsumer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final EntityManager entityManager;

    @Autowired
    public GroupMessageConsumer(ObjectMapper objectMapper,
                                ChatMessageService chatMessageService,
                                UniversalChatHandler chatHandler,
                                KafkaTemplate<String, String> kafkaTemplate,
                                EntityManager entityManager) {
        super(objectMapper, chatMessageService, chatHandler);
        this.kafkaTemplate = kafkaTemplate;
        this.entityManager = entityManager; // добавляем зависимость
    }

    @KafkaListener(topics = "group-messages", groupId = "websocket-group")
    @Transactional
    public void consumeGroupMessage(String kafkaMessage) {
        Long senderId = null;
        Long chatId = null;

        try {
            JsonNode message = objectMapper.readTree(kafkaMessage);
            senderId = message.get("senderId").asLong();
            chatId = message.get("chatId").asLong();
            String text = message.get("text").asText();

            // 1. Сохраняем сообщение в БД
            MessageDto messageDto = new MessageDto();
            messageDto.setChatId(chatId);
            messageDto.setText(text);
            Message savedMessage = chatMessageService.sendMessage(messageDto, senderId);

            // 2. Получаем участников чата
            List<Long> members = chatMessageService.getChatMembers(chatId);
            int deliveredCount = 0;

            // 3. Отправляем сообщение всем участникам
            for (Long memberId : members) {
                if (!memberId.equals(senderId)) {
                    try {
                        if (chatHandler.isUserOnline(memberId)) {
                            // Отправляем через WebSocket
                            sendToSession(
                                    memberId,
                                    formatResponse("GROUP_MESSAGE",
                                            Map.of(
                                                    "chatId", chatId,
                                                    "message", convertToDto(savedMessage)
                                            ))
                            );
                            deliveredCount++;
                        } else {
                            // Отправляем в DLQ для оффлайн-пользователя
                            sendToDlq(kafkaMessage, chatId, memberId);
                        }
                    } catch (Exception e) {
                        logger.error("Failed to send to user {}: {}", memberId, e.getMessage());
                        sendToDlq(kafkaMessage, chatId, memberId);
                    }
                }
            }

            logger.info("Message processed. Chat: {}, Sender: {}, Delivered: {}/{}",
                    chatId, senderId, deliveredCount, members.size() - 1);
        } catch (Exception e) {
            logger.error("Failed to process group message (Chat: {}, Sender: {}): {}",
                    chatId, senderId, e.getMessage());

            if (senderId != null) {
                sendErrorToSender(senderId, "PROCESSING_ERROR", e.getMessage());
            }
        }
    }

    @Transactional
    public void sendToDlq(String originalMessage, Long chatId, Long userId) {
        try {
            // Генерация уникального ключа сообщения
            String messageHash = DigestUtils.md5DigestAsHex(originalMessage.getBytes());

            // Создание новой записи в DeliveryState
            DeliveryState deliveryState = new DeliveryState(chatId, userId, messageHash, originalMessage);
            entityManager.persist(deliveryState); // Сохраняем новую запись в базе данных
            entityManager.flush();                 // Сразу применим изменения

            // Остальной код отправки в DLQ
            String dlqKey = String.format("group_%d_user_%d_%d", chatId, userId, System.currentTimeMillis());
            ObjectNode dlqMessage = objectMapper.createObjectNode();
            dlqMessage.put("chatId", chatId);
            dlqMessage.put("recipientId", userId);
            dlqMessage.put("messageHash", messageHash);
            dlqMessage.put("timestamp", System.currentTimeMillis());
            dlqMessage.set("originalMessage", objectMapper.readTree(originalMessage));

            kafkaTemplate.send("group-messages-dlq", dlqKey, dlqMessage.toString());
            logger.warn("Message sent to DLQ for user {} (chat {}). Hash: {}", userId, chatId, messageHash);
        } catch (Exception e) {
            logger.error("Failed to send message to DLQ", e);
        }
    }
}