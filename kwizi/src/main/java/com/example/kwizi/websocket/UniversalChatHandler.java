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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
@Component
public class UniversalChatHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(UniversalChatHandler.class);

    private final Map<Long, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final UserService userService;

    @Autowired
    public UniversalChatHandler(ObjectMapper objectMapper,
                                KafkaTemplate<String, String> kafkaTemplate,
                                UserService userService) {
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.userService = userService;
    }



    private Long extractUserIdFromUrl(WebSocketSession session) {
        String query = session.getUri().getQuery(); // "id=2"
        return Long.parseLong(query.split("=")[1]);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        logger.info("Received message: {}", message.getPayload());
        Long senderId = extractUserIdFromUrl(session);
        logger.info("Received message from user {}: {}", senderId, message.getPayload());

        try {
            JsonNode originalMessage = objectMapper.readTree(message.getPayload());
            String type = originalMessage.get("type").asText();
            String kafkaMessage;

            if ("PRIVATE".equals(type)) {
                kafkaMessage = String.format(
                        "{\"senderId\":%d, \"type\":\"PRIVATE\", \"recipientId\":%d, \"text\":\"%s\"}",
                        senderId,
                        originalMessage.get("recipientId").asLong(),
                        escapeJsonString(originalMessage.get("text").asText())
                );
                kafkaTemplate.send("private-messages", kafkaMessage);
            }
            else if ("GROUP".equals(type)) {
                kafkaMessage = String.format(
                        "{\"senderId\":%d, \"type\":\"GROUP\", \"chatId\":%d, \"text\":\"%s\"}",
                        senderId,
                        originalMessage.get("chatId").asLong(),
                        escapeJsonString(originalMessage.get("text").asText())
                );
                kafkaTemplate.send("group-messages", kafkaMessage);
            } else {
                sendJsonError(session, "INVALID_TYPE", "Unknown message type");
                return;
            }

            // Подтверждение отправки
            session.sendMessage(new TextMessage(
                    String.format("{\"status\":\"SENT\", \"timestamp\":\"%s\"}",
                            Instant.now().toString())
            ));

        } catch (JsonProcessingException e) {
            sendJsonError(session, "INVALID_JSON", "Malformed JSON message");
        } catch (NoSuchElementException e) {
            sendJsonError(session, "MISSING_FIELD", e.getMessage());
        } catch (Exception e) {
            logger.error("Processing error: {}", e.getMessage());
            sendJsonError(session, "SERVER_ERROR", "Internal server error");
        }
    }
    private String escapeJsonString(String input) {
        return input.replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private void sendJsonError(WebSocketSession session, String code, String message) throws IOException {
        session.sendMessage(new TextMessage(
                String.format("{\"type\":\"ERROR\", \"code\":\"%s\", \"message\":\"%s\"}",
                        code, message)
        ));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = extractUserIdFromUrl(session);
        if (userId != null) {
            activeSessions.remove(userId);
            logger.info("User {} disconnected", userId);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            logger.info("CONNECTED: {}", session.getId());
            Long userId = extractUserIdFromUrl(session);
            activeSessions.put(userId, session);
            logger.info("User {} connected", userId);
            session.setBinaryMessageSizeLimit(1024 * 1024); // 1MB
            session.setTextMessageSizeLimit(1024 * 1024);
            activeSessions.put(extractUserIdFromUrl(session), session);
        } catch (Exception e) {
            logger.error("Failed to init session: {}", e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.warn("Transport error: {}", exception.getMessage()); // Не закрываем сессию явно!
    }
    public Map<Long, WebSocketSession> getActiveSessions() {
        return this.activeSessions;
    }
}