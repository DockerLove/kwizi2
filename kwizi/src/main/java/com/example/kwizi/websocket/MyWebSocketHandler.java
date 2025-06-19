package com.example.kwizi.websocket;

import com.example.kwizi.DTO.internal.*;
import com.example.kwizi.DTO.internal.MessageDto;
import com.example.kwizi.model.Message;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.ChatMemberRepository;
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
    private final ChatMemberRepository chatMemberRepository;
    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Autowired
    public MyWebSocketHandler(ChatMessageService chatMessageService, UserService userService, ObjectMapper objectMapper, ChatMemberRepository chatMemberRepository) {
        this.chatMessageService = chatMessageService;
        this.userService = userService;
        this.objectMapper = objectMapper;
        this.chatMemberRepository = chatMemberRepository;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Вызывается после установления соединения

        // Извлечение ID пользователя из параметров запроса (ws://localhost:8080/ws?id=1)
        Map<String, String> params = getUriTemplateVariables(session);
        Long userId = Long.parseLong(params.get("id"));

        // Сохранение сессии для последующей рассылки
        sessions.put(userId, session);
    }
    //TODO реализованы приватные чаты но не групповые
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            System.out.println("Received message: " + payload);

            // Десериализация JSON в PrivateChatMessageDto
            PrivateChatMessageDto privateChatMessageDto = objectMapper.readValue(payload, PrivateChatMessageDto.class);

            // Получение ID отправителя из параметров сессии
            Long senderId = Long.parseLong(getUriTemplateVariables(session).get("id"));
            User sender = userService.findById(senderId)
                    .orElseThrow(() -> new IllegalArgumentException("Sender not found"));

            // Получение имени пользователя-получателя
            String recipientUsername = privateChatMessageDto.getRecipientUsername();

            // Поиск пользователя-получателя
            User recipient = userService.findByUsername(recipientUsername)
                    .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));

            // Определение chatId (комбинация ID отправителя и получателя)
            Long chatId = generateChatId(senderId, recipient.getId());

            // Создание MessageDto
            MessageDto messageDto = privateChatMessageDto.getMessageDto();
            messageDto.setChatId(chatId); // Устанавливаем chatId в MessageDto

            // Отправка сообщения
            Message savedMessage = chatMessageService.sendPrivateMessage(messageDto, senderId, recipient.getId());
            messageDto.setId(savedMessage.getId());
            messageDto.setCreatedAt(savedMessage.getCreatedAt().toLocalDateTime());
            messageDto.setSenderId(savedMessage.getSender().getId());

            // Рассылка сообщения (только отправителю и получателю)
            broadcastMessageToPrivateChat(messageDto, chatId, senderId, recipient.getId());


        } catch (Exception e) {
            System.err.println("Error processing private message: " + e.getMessage());
            session.sendMessage(new TextMessage("Error: " + e.getMessage()));
            session.close(CloseStatus.SERVER_ERROR);
            throw e;
        }
    }

    private Long generateChatId(Long senderId, Long recipientId) {
        // Ensure IDs are not null
        if (senderId == null || recipientId == null) {
            throw new IllegalArgumentException("SenderId and RecipientId cannot be null");
        }

        if (senderId.equals(recipientId)) {
            throw new IllegalArgumentException("SenderId and RecipientId cannot be the same");
        }

        long chatId;

        if (senderId < recipientId) {
            chatId = senderId * 10000 + recipientId;
        } else {
            chatId = recipientId * 10000 + senderId;
        }
        return chatId;
    }


    private void broadcastMessageToPrivateChat(MessageDto messageDto, Long chatId, Long senderId, Long recipientId) {
        sessions.forEach((userId, session) -> {
            // Проверяем, является ли пользователь отправителем или получателем
            if (userId.equals(senderId) || userId.equals(recipientId)) {
                try {
                    if (session.isOpen()) {
                        String jsonMessage = objectMapper.writeValueAsString(messageDto);
                        session.sendMessage(new TextMessage(jsonMessage));
                    }
                } catch (IOException e) {
                    System.err.println("Error broadcasting message to user " + userId + ": " + e.getMessage());
                }
            } else {
                System.out.println("User " + userId + " is not a member of the private chat " + chatId + ", skipping");
            }
        });
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