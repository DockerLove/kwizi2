package com.example.kwizi.websocket;

import com.example.kwizi.DTO.internal.MessageDto;
import com.example.kwizi.DTO.internal.ChatDto;
import com.example.kwizi.DTO.internal.MessageDto;
import com.example.kwizi.DTO.internal.UserDto;
import com.example.kwizi.model.Message;
import com.example.kwizi.service.ChatMessageService;
import com.example.kwizi.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class MyWebSocketHandler extends TextWebSocketHandler {

    private final ChatMessageService chatMessageService;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // Явный конструктор (вместо @RequiredArgsConstructor)
    @Autowired
    public MyWebSocketHandler(ChatMessageService chatMessageService, UserService userService, ObjectMapper objectMapper) {
        this.chatMessageService = chatMessageService;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Вызывается после установления соединения

        // Извлечение ID пользователя из параметров запроса (ws://localhost:8080/ws?id=1)
        Map<String, String> params = getUriTemplateVariables(session);
        Long userId = Long.parseLong(params.get("id"));

        // Сохранение сессии для последующей рассылки
        sessions.put(userId, session);

        System.out.println("WebSocket connection established");
        System.out.println("User ID from query params: " + userId);
        System.out.println("Session added for user ID: " + userId);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Обработка входящего текстового сообщения

        try {
            String payload = message.getPayload();
            System.out.println("Received message: " + payload);

            // Десериализация JSON в MessageDto (внутренний формат)
            MessageDto messageDto = objectMapper.readValue(payload, MessageDto.class);

            // Получение ID отправителя из параметров сессии (или из аутентификации, в production)
            Long senderId = Long.parseLong(getUriTemplateVariables(session).get("id"));

            // Обработка сообщения сервисом (сохранение в БД, проверка прав доступа и т.д.)
            Message savedMessage = chatMessageService.sendMessage(messageDto, senderId);

            // Преобразование Message в DTO (для ответа клиенту)
            MessageDto messageDtoForSend = convertToDto(savedMessage);

            // Рассылка сообщения всем пользователям в чате
            broadcastMessage(messageDtoForSend, messageDto.getChatId());

        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
            // Отправка ошибки клиенту (опционально)
            session.sendMessage(new TextMessage("Error: " + e.getMessage()));
            session.close(CloseStatus.SERVER_ERROR);
            throw e; // Перебрасываем исключение для обработки Spring
        }
    }

    // Метод для получения параметров из URI
    private Map<String, String> getUriTemplateVariables(WebSocketSession session) {
        if (session.getUri() == null) {
            return null; // Или обработать ошибку
        }
        String uri = session.getUri().toString();
        int startIndex = uri.indexOf("?") + 1;
        if (startIndex <= 0 || uri.length() <= startIndex) {
            return null; // Или обработать ошибку
        }
        String queryParams = uri.substring(startIndex);
        if (queryParams == null || queryParams.isEmpty()) {
            return null; // Или обработать ошибку
        }
        return Arrays.stream(queryParams.split("&"))
                .map(param -> param.split("="))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1]));
    }

    private void broadcastMessage(MessageDto messageDto, Long chatId) {
        // Рассылка сообщения всем пользователям в чате
        sessions.forEach((userId, session) -> {
            try {
                if (session.isOpen()) {
                    // Сериализация DTO в JSON
                    String jsonMessage = objectMapper.writeValueAsString(messageDto);
                    // Отправка сообщения клиенту
                    session.sendMessage(new TextMessage(jsonMessage));
                }
            } catch (IOException e) {
                System.err.println("Error broadcasting message to user " + userId + ": " + e.getMessage());
            }
        });
    }

    private MessageDto convertToDto(Message message) {
        // Преобразование сущности Message в DTO (для ответа клиенту)

        MessageDto messageDto = new MessageDto();
        messageDto.setId(message.getId());
        messageDto.setText(message.getText());
        if (message.getCreatedAt() != null) {
            messageDto.setCreatedAt(message.getCreatedAt().toLocalDateTime());
        }

        // Преобразование связанных объектов в DTO
        if (message.getSender() != null) {
            UserDto userDto = new UserDto();
            userDto.setId(message.getSender().getId());
            userDto.setUsername(message.getSender().getUsername());
            messageDto.setSenderId(userDto.getId());
        }
        if (message.getChat() != null) {
            ChatDto chatDto = new ChatDto();
            chatDto.setId(message.getChat().getId());
            chatDto.setGroupName(message.getChat().getGroupName());
            messageDto.setChatId(chatDto.getId());
        }
        return messageDto;
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // Вызывается после закрытия соединения

        // Извлечение ID пользователя из параметров запроса
        Map<String, String> params = getUriTemplateVariables(session);
        Long userId = Long.parseLong(params.get("id"));

        // Удаление сессии
        sessions.remove(userId);

        System.out.println("WebSocket connection closed for user: " + userId);
    }
}