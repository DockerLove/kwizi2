package com.example.kwizi.scheduler;

import com.example.kwizi.DTO.internal.MessageDto;
import com.example.kwizi.model.DeliveryState;
import com.example.kwizi.model.Message;
import com.example.kwizi.service.ChatMessageService;
import com.example.kwizi.websocket.UniversalChatHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;


//@Component
@Transactional
public class GroupDlqRetryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(GroupDlqRetryScheduler.class);

    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ChatMessageService chatMessageService;
    private final UniversalChatHandler chatHandler;

    //@Autowired
    public GroupDlqRetryScheduler(EntityManager entityManager,
                                  ObjectMapper objectMapper,
                                  KafkaTemplate<String, String> kafkaTemplate,
                                  ChatMessageService chatMessageService,
                                  UniversalChatHandler chatHandler) {
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.chatMessageService = chatMessageService;
        this.chatHandler = chatHandler;
    }

    @Scheduled(fixedDelay = 15_000L) // Повторение каждую минуту
    public void retryUndeliveredMessages() {
        logger.info("GROUP DLQ отправляет сообщение");
        List<DeliveryState> undeliveredStates = findUndeliveredRecords();

        for (DeliveryState state : undeliveredStates) {
            try {
                Long recipientId = state.getRecipientId();

                // Перед отправкой снова проверяем активность пользователя
                if (chatHandler.isUserOnline(recipientId)) {
                    deliverMessage(state);
                    markDelivered(state);
                } else {
                    logger.info("Пользователь {} находится офлайн. Повтор позже.", recipientId);
                }
            } catch (Exception ex) {
                logger.error("Ошибка при попытке повторной отправки сообщения с хэшем {}. Получатель: {}",
                        state.getMessageHash(), state.getRecipientId(), ex);
            }
        }
        logger.info("GROUP DLQ отправил сообщение");
    }

    private List<DeliveryState> findUndeliveredRecords() {
        return entityManager.createQuery(" SELECT ds FROM DeliveryState ds WHERE ds.delivered = false ", DeliveryState.class).getResultList();
    }

    private void deliverMessage(DeliveryState state) throws Exception {
        // Парсим оригинальное сообщение из JSON
        JsonNode originalMessageJson = objectMapper.readTree(state.getOriginalMessage());
        String text = originalMessageJson.get("text").asText();
        Long chatId = originalMessageJson.get("chatId").asLong();
        Long senderId = originalMessageJson.get("senderId").asLong();

        // Создаем объект для отправки пользователю
        String formattedPayload = formatResponse("GROUP_MESSAGE",
                Map.of(
                        "chatId", chatId,
                        "message", convertToDto(new Message()))
        );

        // Вызываем новый метод отправки уведомления через WebSocket
        chatHandler.notifyUserByWebsocket(state.getRecipientId(), formattedPayload);
    }

    private void markDelivered(DeliveryState state) {
        state.setDelivered(true);
        entityManager.merge(state);
    }

    private String formatResponse(String type, Map<String, Object> data) {
        return "{\"type\":\"" + type + "\"," +
                "\"data\":" + objectMapper.valueToTree(data) + "}";
    }

    private MessageDto convertToDto(Message m) {
        MessageDto dto = new MessageDto();
        dto.setId(m.getId());
        dto.setChatId(m.getChat().getId());
        dto.setText(m.getText());
        return dto;
    }
}