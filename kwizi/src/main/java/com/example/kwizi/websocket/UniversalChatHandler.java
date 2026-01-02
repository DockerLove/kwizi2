package com.example.kwizi.websocket;

import com.example.kwizi.DTO.internal.MessageEventDto;
import com.example.kwizi.enums.ChatType;
import com.example.kwizi.exception.ChatNotFoundException;
import com.example.kwizi.exception.MessageValidationException;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.model.Chat;
import com.example.kwizi.repository.ChatMemberRepository;
import com.example.kwizi.repository.ChatRepository;
import com.example.kwizi.repository.UserRepository;
import com.example.kwizi.security.JwtUtils;
import com.example.kwizi.util.MessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.common.lang.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UniversalChatHandler extends TextWebSocketHandler {

    private final Map<Long, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ChatMemberRepository chatMemberRepository;
    private final MessageConverter messageConverter;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final JwtUtils jwtUtils;
    private final Logger logger = LoggerFactory.getLogger(UniversalChatHandler.class);

    @Autowired
    public UniversalChatHandler(ObjectMapper objectMapper,
                                KafkaTemplate<String, String> kafkaTemplate,
                                ChatMemberRepository chatMemberRepository,
                                MessageConverter messageConverter,
                                JwtUtils jwtUtils,ChatRepository chatRepository,
                                UserRepository userRepository) {
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.chatMemberRepository = chatMemberRepository;
        this.messageConverter = messageConverter;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
        this.chatRepository = chatRepository;
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) throws Exception {
        Long senderId = extractUserIdFromToken(session);
        logger.info("Получено сообщение от пользователя {}: {}", senderId, message.getPayload());

        try {
            MessageEventDto event = messageConverter.createMessageEvent(message.getPayload(), senderId);
            logger.info("Обработка события: {}", event.getLogInfo());

            validateEventBeforeSending(event);

            String kafkaMessage = messageConverter.convertToJson(event);
            kafkaTemplate.send(event.getTargetTopic(), kafkaMessage);
            logger.info("Сообщение отправлено в топик: {}", event.getTargetTopic());

            String successResponse = messageConverter.createSuccessResponse();
            session.sendMessage(new TextMessage(successResponse));
            logger.debug("Подтверждение отправлено пользователю {}", senderId);

        } catch (MessageValidationException e) {
            logger.warn("Ошибка валидации от пользователя {}: {}", senderId, e.getMessage());
            String errorResponse = messageConverter.createErrorResponse("VALIDATION_ERROR", e.getMessage());
            session.sendMessage(new TextMessage(errorResponse));
        } catch (UserNotFoundException e) {
            logger.warn("Пользователь не найден: {}", e.getMessage());
            String errorResponse = messageConverter.createErrorResponse("USER_NOT_FOUND", e.getMessage());
            session.sendMessage(new TextMessage(errorResponse));
        } catch (AccessDeniedException e) {
            logger.warn("Вы не являетесь участником чата: {}", e.getMessage());
            String errorResponse = messageConverter.createErrorResponse("ACCESS_DENIED", e.getMessage());
            session.sendMessage(new TextMessage(errorResponse));
        }catch (ChatNotFoundException e) {
            logger.warn("Чат не найден: {}", e.getMessage());
            String errorResponse = messageConverter.createErrorResponse("CHAT_NOT_FOUND", e.getMessage());
            session.sendMessage(new TextMessage(errorResponse));
        } catch (Exception e) {
            logger.error("Ошибка обработки сообщения от пользователя {}: {}", senderId, e.getMessage(), e);
            String errorResponse = messageConverter.createErrorResponse("SERVER_ERROR", "Внутренняя ошибка сервера");
            session.sendMessage(new TextMessage(errorResponse));
        }
    }


    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        try {
            Long userId = extractUserIdFromToken(session);
            activeSessions.put(userId, session);

            session.setBinaryMessageSizeLimit(1024 * 1024);
            session.setTextMessageSizeLimit(1024 * 1024);

            logger.info("Пользователь {} подключился. Session ID: {}", userId, session.getId());

        } catch (Exception e) {
            logger.warn("Ошибка аутентификации WebSocket-сессии: {}", e.getMessage());
            try {
                session.close(CloseStatus.BAD_DATA);
            } catch (IOException ioEx) {
                logger.debug("Не удалось закрыть сессию после ошибки аутентификации", ioEx);
            }
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        try {
            Long userId = extractUserIdFromToken(session);
            activeSessions.remove(userId);
            logger.info("Пользователь {} отключился. Причина: {}", userId, status);
        } catch (Exception e) {
            logger.debug("Закрытие неаутентифицированной сессии");
        }
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) {
        try {
            Long userId = extractUserIdFromToken(session);
            logger.warn("Ошибка транспорта для пользователя {}: {}", userId, exception.getMessage());
            safelyCloseUserSession(userId);
        } catch (Exception e) {
            logger.debug("Транспортная ошибка в неаутентифицированной сессии");
        }
    }

    private void validateEventBeforeSending(MessageEventDto event) {

        if (event.isPrivate()) {
            if (!userRepository.existsById(event.getRecipientId())) {
                throw new UserNotFoundException("Получатель с ID " + event.getRecipientId() + " не найден");
            }
        } else if (event.isGroup()) {

            if (event.getChatId() == null) {
                throw new MessageValidationException("Для группового сообщения обязателен chatId");
            }

            Optional<Chat> chat = chatRepository.findById(event.getChatId());
            if (chat.isPresent()) {
                if (chat.get().getChatType() == ChatType.PRIVATE) {
                    throw new MessageValidationException("Нельзя отправить групповое сообщение в приватный чат");
                }
            }

            if (!chatRepository.existsById(event.getChatId())) {
                throw new ChatNotFoundException("Чат с ID " + event.getChatId() + " не найден");
            }

            if (!chatMemberRepository.existsByChatIdAndUserId(event.getChatId(), event.getSenderId())) {
                throw new AccessDeniedException("Вы не являетесь участником этого чата");
            }
        }
    }

    private Long extractUserIdFromToken(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null || uri.getQuery() == null) {
            throw new IllegalArgumentException("Отсутствует URI или query");
        }

        String query = uri.getQuery();
        String token = null;
        for (String param : query.split("&")) {
            if (param.startsWith("token=")) {
                token = param.substring(6);
                break;
            }
        }

        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Параметр 'token' отсутствует");
        }

        if (jwtUtils.isTokenRevoked(token)) {
            throw new IllegalArgumentException("Токен отозван");
        }

        return jwtUtils.getUserIdFromToken(token);
    }

    private void safelyCloseUserSession(Long userId) {
        WebSocketSession session = activeSessions.remove(userId);
        if (session != null && session.isOpen()) {
            try {
                session.close(CloseStatus.SESSION_NOT_RELIABLE);
                logger.info("Сессия пользователя {} принудительно закрыта", userId);
            } catch (IOException e) {
                logger.debug("Ошибка при закрытии сессии пользователя {}", userId, e);
            }
        }
    }

    public void closeUserSession(Long userId) {
        WebSocketSession session = activeSessions.remove(userId);
        if (session != null && session.isOpen()) {
            try {
                session.close(CloseStatus.GOING_AWAY);
                logger.info("WebSocket-сессия пользователя {} закрыта при logout", userId);
            } catch (IOException e) {
                logger.warn("Ошибка при закрытии WebSocket-сессии пользователя {}", userId, e);
            }
        } else {
            logger.debug("Пользователь {} не имеет активной WebSocket-сессии", userId);
        }
    }

    public void sendToUser(Long userId, String payload) {
        WebSocketSession session = activeSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(payload));
                logger.debug("Сообщение отправлено пользователю {}", userId);
            } catch (IOException e) {
                logger.warn("Ошибка отправки сообщения пользователю {}. Удаляем сессию.", userId, e);
                activeSessions.remove(userId);
            }
        }
    }

    public boolean isUserOnline(Long userId) {
        WebSocketSession session = activeSessions.get(userId);
        return session != null && session.isOpen();
    }

    public void sendToUser(Long userId, Object messageObject) {
        try {
            String payload = objectMapper.writeValueAsString(messageObject);
            sendToUser(userId, payload);
        } catch (Exception e) {
            logger.error("Ошибка сериализации сообщения для пользователя {}: {}", userId, e.getMessage());
        }
    }

    public void broadcastToChat(Long chatId, String payload) {
        List<Long> chatMemberIds = getChatMemberIds(chatId);
        int sentCount = 0;
        for (Long memberId : chatMemberIds) {
            if (isUserOnline(memberId)) {
                sendToUser(memberId, payload);
                sentCount++;
            }
        }
        logger.info("Трансляция в чат {} завершена. Отправлено {}/{} пользователям", chatId, sentCount, chatMemberIds.size());
    }

    private List<Long> getChatMemberIds(Long chatId) {
        return chatMemberRepository.findUserIdsByChatId(chatId);
    }
}