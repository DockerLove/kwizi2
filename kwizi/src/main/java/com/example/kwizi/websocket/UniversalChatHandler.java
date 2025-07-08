package com.example.kwizi.websocket;

import com.example.kwizi.DTO.internal.MessageDto;
import com.example.kwizi.DTO.request.ChatMessageRequest;
import com.example.kwizi.DTO.request.GroupMessageRequest;
import com.example.kwizi.DTO.request.PrivateMessageRequest;
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
        logger.info("Получено сообщение от пользователя с ID: {}.  Сообщение: {}", senderId, message.getPayload());

        try {
            JsonNode rootNode = objectMapper.readTree(message.getPayload());
            MessageType type = MessageType.valueOf(rootNode.get("type").asText());

            if (type == MessageType.PRIVATE) {
                PrivateMessageRequest request = objectMapper.treeToValue(rootNode, PrivateMessageRequest.class);
                handlePrivateMessage(senderId, request);
            } else if (type == MessageType.GROUP) { // Added check for GROUP type
                GroupMessageRequest request = objectMapper.treeToValue(rootNode, GroupMessageRequest.class);
                handleGroupMessage(senderId, request);
            } else {
                logger.warn("Неизвестный тип сообщения от пользователя с ID {}: {}", senderId, type);
                session.sendMessage(new TextMessage("Ошибка: Неизвестный тип сообщения.")); // Inform the client
            }
        } catch (IllegalArgumentException e) { // Catch IllegalArgumentException from MessageType.valueOf
            logger.error("Ошибка при обработке сообщения от пользователя с ID {}: Неверный формат сообщения: {}", senderId, e.getMessage());
            session.sendMessage(new TextMessage("Ошибка: Неверный формат сообщения.")); // Inform the client
        }
        catch (Exception e) {
            logger.error("Ошибка при обработке сообщения от пользователя с ID {}: {}", senderId, e.getMessage(), e);
            session.sendMessage(new TextMessage("Ошибка: " + e.getMessage()));
        }
    }

    private void handlePrivateMessage(Long senderId, PrivateMessageRequest request) throws IOException {
        try {
            Long recipientId = request.getRecipientId();
            logger.info("Обработка личного сообщения от {} для {}", senderId, recipientId);
            User recipient = userService.findById(recipientId)
                    .orElseThrow(() -> {
                        logger.warn("Получатель не найден: {}", recipientId);
                        return new UserNotFoundException("Получатель не найден");
                    });

            MessageDto messageDto = new MessageDto();
            messageDto.setText(request.getText());

            Message savedMessage = chatMessageService.sendPrivateMessage(messageDto, senderId, recipientId);

            sendToUser(senderId, savedMessage);
            sendToUser(recipientId, savedMessage);
            logger.info("Личное сообщение от {} для {} успешно обработано", senderId, recipientId);
        } catch (UserNotFoundException e) {
            logger.warn("Ошибка при обработке личного сообщения от {}: {}", senderId, e.getMessage());
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
            MessageDto messageDto = new MessageDto();
            messageDto.setChatId(chatId);
            messageDto.setText(request.getText());

            Message savedMessage = chatMessageService.sendMessage(messageDto, senderId);

            chatMessageService.getChatMembers(chatId).forEach(userId -> {
                try {
                    sendToUser(userId, savedMessage);
                } catch (IOException e) {
                    logger.error("Ошибка при отправке сообщения пользователю {} в чате {}: {}", userId, chatId, e.getMessage(), e); // Log the exception
                    // Consider handling the error, possibly by notifying the sender.
                }
            });
            logger.info("Групповое сообщение от {} для чата {} успешно обработано", senderId, chatId);
        } catch (Exception e) {
            logger.error("Ошибка при обработке группового сообщения от {}: {}", senderId, e.getMessage(), e);
            // Consider sending an error message back to the sender.
        }
    }

    private void sendToUser(Long userId, Message message) throws IOException {
        WebSocketSession session = activeSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                        Map.of(
                                "type", "MESSAGE",
                                "data", convertToDto(message)
                        )
                )));
                logger.debug("Сообщение отправлено пользователю с ID: {}", userId);
            } catch (IOException e) {
                logger.error("Ошибка при отправке сообщения пользователю с ID {}: {}", userId, e.getMessage(), e); // Log the exception
                // Consider removing the session from activeSessions if the send fails.
            }
        } else {
            logger.warn("Не удалось отправить сообщение пользователю с ID {}: сессия не найдена или закрыта", userId);
            // Consider removing the session from activeSessions if the send fails.
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