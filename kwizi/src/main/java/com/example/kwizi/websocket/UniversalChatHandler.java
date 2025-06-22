package com.example.kwizi.websocket;

import com.example.kwizi.DTO.internal.MessageDto;
import com.example.kwizi.DTO.request.ChatMessageRequest;
import com.example.kwizi.DTO.request.GroupMessageRequest;
import com.example.kwizi.DTO.request.PrivateMessageRequest;
import com.example.kwizi.enums.MessageType;
import com.example.kwizi.model.Message;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.ChatMemberRepository;
import com.example.kwizi.service.ChatMessageService;
import com.example.kwizi.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    }

    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            Long senderId = extractUserId(session);
            JsonNode rootNode = objectMapper.readTree(message.getPayload());
            MessageType type = MessageType.valueOf(rootNode.get("type").asText());

            if (type == MessageType.PRIVATE) {
                PrivateMessageRequest request = objectMapper.treeToValue(rootNode, PrivateMessageRequest.class);
                handlePrivateMessage(senderId, request);
            } else {
                GroupMessageRequest request = objectMapper.treeToValue(rootNode, GroupMessageRequest.class);
                handleGroupMessage(senderId, request);
            }
        } catch (Exception e) {
            session.sendMessage(new TextMessage("Ошибка: " + e.getMessage()));
        }
    }
    //todo реализовать логику создания личного чата
    private void handlePrivateMessage(Long senderId, PrivateMessageRequest request) throws IOException {
        User recipient = userService.findById(request.getRecipientId())
                .orElseThrow(() -> new IllegalArgumentException("Получатель не найден"));

        MessageDto messageDto = new MessageDto();
        messageDto.setText(request.getText());

        Message savedMessage = chatMessageService.sendPrivateMessage(messageDto, senderId, request.getRecipientId());

        sendToUser(senderId, savedMessage);
        sendToUser(request.getRecipientId(), savedMessage);
    }

    private void handleGroupMessage(Long senderId, GroupMessageRequest request) throws IOException {
        MessageDto messageDto = new MessageDto();
        messageDto.setChatId(request.getChatId());
        messageDto.setText(request.getText());

        Message savedMessage = chatMessageService.sendMessage(messageDto, senderId);

        chatMessageService.getChatMembers(request.getChatId()).forEach(userId -> {
            try {
                sendToUser(userId, savedMessage);
            } catch (IOException e) {
                // Логирование ошибки
            }
        });
    }

    private void sendToUser(Long userId, Message message) throws IOException {
        WebSocketSession session = activeSessions.get(userId);
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                    Map.of(
                            "type", "MESSAGE",
                            "data", convertToDto(message)
                    )
            )));
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
        return Long.parseLong(query.split("id=")[1].split("&")[0]);
    }

    private void sendSystemMessage(WebSocketSession session, String text) throws IOException {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                Map.of("type", "SYSTEM", "text", text)
        )));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = extractUserId(session);
        activeSessions.remove(userId);
    }
}
