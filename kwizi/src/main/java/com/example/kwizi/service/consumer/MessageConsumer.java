package com.example.kwizi.service.consumer;

import com.example.kwizi.DTO.internal.MessageDto;
import com.example.kwizi.DTO.internal.MessageEventDto;
import com.example.kwizi.model.Message;
import com.example.kwizi.service.ChatMessageService;
import com.example.kwizi.util.MessageConverter;
import com.example.kwizi.websocket.UniversalChatHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
public class MessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(MessageConsumer.class);

    private final ObjectMapper objectMapper;
    private final MessageConverter messageConverter;
    private final ChatMessageService chatMessageService;
    private final UniversalChatHandler chatHandler;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public MessageConsumer(ObjectMapper objectMapper, MessageConverter messageConverter,
                           ChatMessageService chatMessageService, UniversalChatHandler chatHandler,
                           KafkaTemplate<String, String> kafkaTemplate) {
        this.objectMapper = objectMapper;
        this.messageConverter = messageConverter;
        this.chatMessageService = chatMessageService;
        this.chatHandler = chatHandler;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = {"private-messages", "group-messages"}, groupId = "message-consumer-group")
    @Transactional
    public void consumeMessage(String kafkaMessage,
                               @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        MessageEventDto event = null;

        try {
            event = messageConverter.convertToEvent(kafkaMessage);
            logger.info("Получено сообщение из топика {}: {}", topic, event.getLogInfo());

            Message savedMessage;
            if (event.isPrivate()) {
                savedMessage = processPrivateMessage(event);
            } else {
                savedMessage = processGroupMessage(event);
            }

            logger.info("Сообщение успешно обработано: {}", savedMessage.getId());

        } catch (Exception e) {
            logger.error("Ошибка обработки сообщения: {}", event != null ? event.getLogInfo() : "unknown", e);
            throw new RuntimeException("Ошибка обработки Kafka сообщения", e);
        }
    }

    private Message processPrivateMessage(MessageEventDto event) {
        logger.debug("Обработка приватного сообщения: {} -> {}",
                event.getSenderId(), event.getRecipientId());

        MessageDto messageDto = createMessageDto(event);
        Message savedMessage = chatMessageService.sendPrivateMessage(
                messageDto, event.getSenderId(), event.getRecipientId());

        if (chatHandler.isUserOnline(event.getRecipientId())) {
            MessageDto responseDto = messageConverter.convertToDto(savedMessage);
            sendPrivateMessageToRecipient(event.getRecipientId(), responseDto);
            sendDeliveryConfirmation(event.getSenderId(), savedMessage.getId(), "DELIVERED");
        } else {
            logger.warn("Получатель {} оффлайн. Отправляем сообщение в DLQ", event.getRecipientId());
            sendToPrivateDlq(event, savedMessage.getId());
            sendDeliveryConfirmation(event.getSenderId(), savedMessage.getId(), "SAVED_OFFLINE");
        }

        return savedMessage;
    }

    private Message processGroupMessage(MessageEventDto event) {
        logger.debug("Обработка группового сообщения: {} -> чат {}",
                event.getSenderId(), event.getChatId());

        MessageDto messageDto = createMessageDto(event);
        messageDto.setChatId(event.getChatId());
        Message savedMessage = chatMessageService.sendMessage(messageDto, event.getSenderId());

        List<Long> members = chatMessageService.getChatMembers(event.getChatId());

        sendGroupMessageToMembers(event, savedMessage, members);

        return savedMessage;
    }

    private void sendGroupMessageToMembers(MessageEventDto event, Message savedMessage, List<Long> members) {
        try {
            MessageDto messageDto = messageConverter.convertToDto(savedMessage);

            int deliveredCount = 0;
            int offlineCount = 0;

            for (Long memberId : members) {
                if (!memberId.equals(event.getSenderId())) {
                    if (chatHandler.isUserOnline(memberId)) {
                        Map<String, Object> response = Map.of(
                                "type", "GROUP_MESSAGE",
                                "chatId", event.getChatId(),
                                "message", messageDto
                        );
                        chatHandler.sendToUser(memberId, response);
                        deliveredCount++;
                    } else {
                        logger.debug("Участник {} оффлайн, отправляем в DLQ", memberId);
                        sendGroupMessageToDlq(event, savedMessage.getId(), memberId);
                        offlineCount++;
                    }
                }
            }

            logger.info("Групповое сообщение доставлено {}/{} участникам. Оффлайн: {}",
                    deliveredCount, members.size() - 1, offlineCount);

        } catch (Exception e) {
            logger.error("Ошибка отправки группового сообщения в чат {}", event.getChatId(), e);
        }
    }

    private void sendGroupMessageToDlq(MessageEventDto event, Long messageId, Long recipientId) {
        try {
            Map<String, Object> dlqMessage = Map.of(
                    "originalEvent", event,
                    "messageId", messageId,
                    "recipientId", recipientId,
                    "chatId", event.getChatId(),
                    "timestamp", System.currentTimeMillis(),
                    "reason", "USER_OFFLINE",
                    "messageType", "GROUP"
            );

            String dlqPayload = objectMapper.writeValueAsString(dlqMessage);
            kafkaTemplate.send("group-messages-dlq", dlqPayload);

            logger.info("Групповое сообщение {} отправлено в group-messages-dlq для пользователя {}",
                    messageId, recipientId);

        } catch (Exception e) {
            logger.error("Ошибка отправки группового сообщения в DLQ: {}", e.getMessage(), e);
        }
    }

    private void sendToPrivateDlq(MessageEventDto event, Long messageId) {
        try {
            Map<String, Object> dlqMessage = Map.of(
                    "originalEvent", event,
                    "messageId", messageId,
                    "recipientId", event.getRecipientId(),
                    "timestamp", System.currentTimeMillis(),
                    "reason", "USER_OFFLINE"
            );

            String dlqPayload = objectMapper.writeValueAsString(dlqMessage);
            kafkaTemplate.send("private-messages-dlq", dlqPayload);

            logger.info("Сообщение {} отправлено в private-messages-dlq для пользователя {}",
                    messageId, event.getRecipientId());

        } catch (Exception e) {
            logger.error("Ошибка отправки сообщения в DLQ: {}", e.getMessage(), e);
        }
    }

    private void sendDeliveryConfirmation(Long senderId, Long messageId, String status) {
        try {
            Map<String, Object> confirmation = Map.of(
                    "type", "DELIVERY_CONFIRMATION",
                    "messageId", messageId,
                    "status", status
            );
            chatHandler.sendToUser(senderId, confirmation);
            logger.debug("Подтверждение доставки отправлено пользователю {}: {}", senderId, status);
        } catch (Exception e) {
            logger.warn("Не удалось отправить подтверждение доставки пользователю {}", senderId, e);
        }
    }

    private MessageDto createMessageDto(MessageEventDto event) {
        MessageDto dto = new MessageDto();
        dto.setText(event.getText());
        return dto;
    }

    private void sendPrivateMessageToRecipient(Long recipientId, MessageDto messageDto) {
        try {
            Map<String, Object> response = Map.of(
                    "type", "PRIVATE_MESSAGE",
                    "message", messageDto
            );
            chatHandler.sendToUser(recipientId, response);
            logger.debug("Приватное сообщение отправлено пользователю {}", recipientId);
        } catch (Exception e) {
            logger.error("Ошибка отправки приватного сообщения пользователю {}", recipientId, e);
        }
    }
}