package com.example.kwizi.websocket;

import com.example.kwizi.DTO.internal.MessageDto;
import com.example.kwizi.DTO.request.ChatMessageRequest;
import com.example.kwizi.DTO.request.GroupMessageRequest;
import com.example.kwizi.DTO.request.PrivateMessageRequest;
import com.example.kwizi.DTO.response.WebSocketResponse;
import com.example.kwizi.enums.MessageType;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.model.Message;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.ChatMemberRepository;
import com.example.kwizi.service.ChatMessageService;
import com.example.kwizi.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Component
public class UniversalChatHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(UniversalChatHandler.class);

    private final Map<Long, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final ChatMessageService chatMessageService;
    private final UserService userService;

    @Autowired
    public UniversalChatHandler(ObjectMapper objectMapper,
                                ChatMessageService chatMessageService,
                                UserService userService) {
        this.objectMapper = objectMapper;
        this.chatMessageService = chatMessageService;
        this.userService = userService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = extractUserId(session);
        activeSessions.put(userId, session);
        sendSystemMessage(session, "Вы успешно подключились. ID: " + userId);
        logger.info("Установлено новое соединение WebSocket.  ID пользователя: {}", userId);
    }

    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long senderId = extractUserId(session);
        logger.info("Получено сообщение от пользователя с ID: {}. Сообщение: {}", senderId, message.getPayload());

        try {
            JsonNode rootNode = objectMapper.readTree(message.getPayload());
            MessageType type = MessageType.valueOf(rootNode.get("type").asText());

            if (type == MessageType.PRIVATE) {
                PrivateMessageRequest request = objectMapper.treeToValue(rootNode, PrivateMessageRequest.class);
                handlePrivateMessage(senderId, request);
            } else if (type == MessageType.GROUP) {
                GroupMessageRequest request = objectMapper.treeToValue(rootNode, GroupMessageRequest.class);
                handleGroupMessage(senderId, request);
            } else {
                sendJsonError(session, "UNKNOWN_TYPE", "Неизвестный тип сообщения");
            }
        } catch (IllegalArgumentException e) {
            sendJsonError(session, "INVALID_FORMAT", "Неверный формат сообщения");
        } catch (UserNotFoundException e) {
            sendJsonError(session, "USER_NOT_FOUND", e.getMessage());
        } catch (Exception e) {
            sendJsonError(session, "SERVER_ERROR", e.getMessage());
        }
    }

    // Универсальный метод для отправки ошибок
    private void sendJsonError(WebSocketSession session, String errorType, String message) throws IOException {
        WebSocketResponse response = new WebSocketResponse("ERROR", Map.of(
                "code", errorType,
                "message", message
        ));
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    private void handlePrivateMessage(Long senderId, PrivateMessageRequest request) {
        try {
            Long recipientId = request.getRecipientId();
            if (senderId.equals(request.getRecipientId())) {
                sendToUser(senderId, new WebSocketResponse("ERROR", "SELF_MESSAGE", "Нельзя писать самому себе"));
                return;
            }

            logger.info("Обработка личного сообщения от {} для {}", senderId, recipientId);
            User recipient = userService.findById(recipientId)
                    .orElseThrow(() -> {
                        logger.warn("Получатель не найден: {}", recipientId);
                        return new UserNotFoundException("Получатель не найден");
                    });

            MessageDto messageDto = new MessageDto();
            messageDto.setText(request.getText());

            Message savedMessage = chatMessageService.sendPrivateMessage(messageDto, senderId, recipientId);

            sendToUser(senderId, new WebSocketResponse("MESSAGE", savedMessage));
            sendToUser(recipientId, new WebSocketResponse("MESSAGE", savedMessage));
            logger.info("Личное сообщение от {} для {} успешно обработано", senderId, recipientId);
        } catch (UserNotFoundException e) {
            logger.warn("Ошибка при обработке личного сообщения от {}: {}", senderId, e.getMessage());
            throw new UserNotFoundException(e.getMessage());
            // Ideally, inform the sender that the recipient is not found (e.g., via sendToUser with an error message).
        } catch (Exception e) {
            logger.error("Ошибка при обработке личного сообщения от {}: {}", senderId, e.getMessage(), e);
            // Consider sending an error message back to the sender.
        }
    }

    private void handleGroupMessage(Long senderId, GroupMessageRequest request) throws IOException {
        try {
            Long chatId = request.getChatId();
            logger.info("Обработка группового сообщения от {} для чата {}", senderId, chatId);

            // 1. Создаем и сохраняем сообщение
            MessageDto messageDto = new MessageDto();
            messageDto.setChatId(chatId);
            messageDto.setText(request.getText());
            Message savedMessage = chatMessageService.sendMessage(messageDto, senderId);

            // 2. Конвертируем в DTO перед отправкой
            MessageDto messageResponse = convertToDto(savedMessage);

            // 3. Отправляем сообщение всем участникам
            List<Long> chatMembers = chatMessageService.getChatMembers(chatId);
            List<Long> offlineUsers = new ArrayList<>();
            for (Long userId : chatMembers) {
                if (!activeSessions.containsKey(userId)) {
                    offlineUsers.add(userId);
                    continue;
                }
                try {

                    WebSocketSession session = activeSessions.get(userId);
                    if (session == null || !session.isOpen()) {
                        logger.warn("Пользователь {} не имеет активной сессии", userId);
                        sendToUser(senderId, new WebSocketResponse(
                                "WARNING",
                                Map.of(
                                        "code", "USER_OFFLINE",
                                        "message", "Пользователь " + userId + " не в сети",
                                        "userId", userId
                                )
                        ));
                        continue;
                    }
                    if(senderId!=userId) {
                        sendToUser(userId, new WebSocketResponse("MESSAGE", messageResponse));
                    }
                } catch (IOException e) {
                    logger.error("Ошибка отправки пользователю {}: {}", userId, e.getMessage());
                    sendToUser(senderId, new WebSocketResponse(
                            "ERROR",
                            Map.of(
                                    "code", "DELIVERY_FAILED",
                                    "message", "Не удалось доставить сообщение пользователю " + userId,
                                    "userId", userId
                            )
                    ));
                }
            }

            if (!offlineUsers.isEmpty()) {
                sendToUser(senderId, new WebSocketResponse("INFO",
                        Map.of("code","OFFLINE_USERS",
                        "message","Сообщение будет доставлено при подключении",
                        "userIds", offlineUsers
                        )));
            }


                logger.info("Сообщение успешно отправлено в чат {}", chatId);

        } catch (IllegalArgumentException e) {
            sendToUser(senderId, new WebSocketResponse(
                    "ERROR",
                    Map.of(
                            "code", "CHAT_NOT_FOUND",
                            "message", e.getMessage()
                    )
            ));
        } catch (Exception e) {
            logger.error("Критическая ошибка: {}", e.getMessage());
            sendToUser(senderId, new WebSocketResponse(
                    "ERROR",
                    Map.of(
                            "code", "SERVER_ERROR",
                            "message", "Внутренняя ошибка сервера: " + e.getMessage()
                    )
            ));
        }
    }



    private void sendToUser(Long userId, WebSocketResponse response) throws IOException {
        WebSocketSession session = activeSessions.get(userId);
        if (session != null && session.isOpen()) {
            String jsonResponse = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(jsonResponse));
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

    private Long extractUserId(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("id=")) {
            try {
                return Long.parseLong(query.split("id=")[1].split("&")[0]);
            } catch (NumberFormatException e) {
                logger.warn("Ошибка при извлечении userId из query: {}", e.getMessage());
                return null; // or throw an exception
            }
        }
        logger.warn("Не удалось извлечь userId из URL. Query: {}", query);
        return null; // or throw an exception
    }

    private void sendSystemMessage(WebSocketSession session, String text) throws IOException {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                    Map.of("type", "SYSTEM", "text", text)
            )));
            logger.debug("Системное сообщение отправлено: {}", text);
        } catch (IOException e) {
            logger.error("Ошибка при отправке системного сообщения: {}. Ошибка: {}", text, e.getMessage(), e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = extractUserId(session);
        if (userId != null) {
            activeSessions.remove(userId);
            logger.info("Соединение WebSocket закрыто.  ID пользователя: {}, Status: {}", userId, status);
        } else {
            logger.warn("Соединение WebSocket закрыто, но не удалось определить ID пользователя.  Status: {}", status);
        }
    }
}