package com.example.kwizi.websocket;

import com.example.kwizi.DTO.internal.MessageEventDto;
import com.example.kwizi.exception.MessageValidationException;
import com.example.kwizi.repository.ChatMemberRepository;
import com.example.kwizi.util.MessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.common.lang.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UniversalChatHandler extends TextWebSocketHandler {

    private final Map<Long, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ChatMemberRepository chatMemberRepository;
    private final MessageConverter messageConverter;
    private final Logger logger = LoggerFactory.getLogger(UniversalChatHandler.class);

    @Autowired
    public UniversalChatHandler(ObjectMapper objectMapper,
                                KafkaTemplate<String, String> kafkaTemplate,
                                ChatMemberRepository chatMemberRepository,
                                MessageConverter messageConverter) {
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.chatMemberRepository = chatMemberRepository;
        this.messageConverter = messageConverter;
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) throws Exception {
        Long senderId = extractUserIdFromUrl(session);
        logger.info("Получено сообщение от пользователя {}: {}", senderId, message.getPayload());

        try {
            // 1. Создаем MessageEventDto из JSON клиента
            MessageEventDto event = messageConverter.createMessageEvent(message.getPayload(), senderId);

            logger.info("Обработка события: {}", event.getLogInfo());

            // 2. Отправляем в Kafka
            String kafkaMessage = messageConverter.convertToJson(event);
            kafkaTemplate.send(event.getTargetTopic(), kafkaMessage);

            logger.info("Сообщение отправлено в топик: {}", event.getTargetTopic());

            // 3. Отправляем подтверждение клиенту
            String successResponse = messageConverter.createSuccessResponse();
            session.sendMessage(new TextMessage(successResponse));

            logger.debug("Подтверждение отправлено пользователю {}", senderId);

        } catch (MessageValidationException e) {
            logger.warn("Ошибка валидации от пользователя {}: {}", senderId, e.getMessage());
            String errorResponse = messageConverter.createErrorResponse("VALIDATION_ERROR", e.getMessage());
            session.sendMessage(new TextMessage(errorResponse));

        } catch (Exception e) {
            logger.error("Ошибка обработки сообщения от пользователя {}: {}", senderId, e.getMessage(), e);
            String errorResponse = messageConverter.createErrorResponse("SERVER_ERROR", "Внутренняя ошибка сервера");
            session.sendMessage(new TextMessage(errorResponse));
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session,@NonNull CloseStatus status) {
        Long userId = extractUserIdFromUrl(session);
        if (userId != null) {
            activeSessions.remove(userId);
            logger.info("Пользователь {} отключился. Причина: {}", userId, status.toString());
        }
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        try {
            Long userId = extractUserIdFromUrl(session);
            activeSessions.put(userId, session);

            // Настройка лимитов сообщений
            session.setBinaryMessageSizeLimit(1024 * 1024); // 1MB
            session.setTextMessageSizeLimit(1024 * 1024);

            logger.info("Пользователь {} подключился. ID сессии: {}", userId, session.getId());

        } catch (Exception e) {
            logger.error("Ошибка инициализации WebSocket сессии: {}", e.getMessage(), e);
        }
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) {
        Long userId = extractUserIdFromUrl(session);
        logger.warn("Ошибка транспорта WebSocket для пользователя {}: {}", userId, exception.getMessage());

        if (userId != null) {
            safelyCloseUserSession(userId);
        }
    }

    // Упрощенный метод отправки сообщения пользователю

    public void sendToUser(Long userId, String payload) {
        WebSocketSession session = activeSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(payload));
                logger.debug("Сообщение отправлено пользователю {}", userId);
            } catch (IOException e) {
                logger.warn("Ошибка отправки WebSocket сообщения пользователю {}. Удаляем сессию. Ошибка: {}",
                        userId, e.getMessage());
                activeSessions.remove(userId); // Удаляем битую сессию
            }
        } else {
            logger.debug("Пользователь {} не в сети или сессия закрыта", userId);
        }
    }
    public boolean isUserOnline(Long userId) {
        WebSocketSession session = activeSessions.get(userId);
        return session != null && session.isOpen();
    }

    // Метод для отправки структурированных сообщений (используется в консьюмерах)
    public void sendToUser(Long userId, Object messageObject) {
        try {
            String payload = objectMapper.writeValueAsString(messageObject);
            sendToUser(userId, payload);
        } catch (Exception e) {
            logger.error("Ошибка сериализации сообщения для пользователя {}: {}", userId, e.getMessage());
        }
    }

    // Broadcast для групповых чатов (оставляем для обратной совместимости)
    public void broadcastToChat(Long chatId, String payload) {
        logger.info("Трансляция в чат {}: {}", chatId, payload);

        List<Long> chatMemberIds = getChatMemberIds(chatId);
        int sentCount = 0;

        for (Long memberId : chatMemberIds) {
            if (isUserOnline(memberId)) {
                sendToUser(memberId, payload);
                sentCount++;
            }
        }

        logger.info("Трансляция завершена. Отправлено {}/{} пользователям", sentCount, chatMemberIds.size());
    }

    private List<Long> getChatMemberIds(Long chatId) {
        return chatMemberRepository.findUserIdsByChatId(chatId);
    }

    private Long extractUserIdFromUrl(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();
            if (query == null || !query.startsWith("id=")) {
                throw new IllegalArgumentException("WebSocket URL must contain 'id' parameter");
            }

            String userIdStr = query.substring(3);
            Long userId = Long.parseLong(userIdStr);

            if (userId <= 0) {
                throw new IllegalArgumentException("User ID must be positive");
            }

            return userId;
        } catch (Exception e) {
            logger.error("Не удалось извлечь ID пользователя из URL: {}", session.getUri(), e);
            throw new RuntimeException("Неверный формат URL WebSocket", e);
        }
    }

    private void safelyCloseUserSession(Long userId) {
        WebSocketSession session = activeSessions.remove(userId);
        if (session != null && session.isOpen()) {
            try {
                session.close(CloseStatus.SESSION_NOT_RELIABLE);
                logger.info("Сессия пользователя {} закрыта из-за ошибки", userId);
            } catch (IOException e) {
                logger.debug("Ошибка при закрытии сессии пользователя {}", userId, e);
            }
        }
    }
}