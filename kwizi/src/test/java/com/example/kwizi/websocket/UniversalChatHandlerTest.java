package com.example.kwizi.websocket;

import com.example.kwizi.DTO.internal.MessageEventDto;
import com.example.kwizi.enums.MessageType;
import com.example.kwizi.exception.MessageValidationException;
import com.example.kwizi.repository.ChatMemberRepository;
import com.example.kwizi.util.MessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UniversalChatHandlerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ChatMemberRepository chatMemberRepository;

    @Mock
    private MessageConverter messageConverter;

    @Mock
    private WebSocketSession session;

    @Captor
    private ArgumentCaptor<TextMessage> textMessageCaptor;

    @InjectMocks
    private UniversalChatHandler chatHandler;

    private static final Long TEST_USER_ID = 123L;

    // ✅ Убираем общий setUp и создаем session для каждого теста отдельно
    private WebSocketSession createMockSession(Long userId) throws Exception {
        WebSocketSession mockSession = mock(WebSocketSession.class);
        URI uri = new URI("ws://localhost:8080/chat?id=" + userId);
        when(mockSession.getUri()).thenReturn(uri);
        when(mockSession.isOpen()).thenReturn(true);
        when(mockSession.getId()).thenReturn("session-" + userId);
        return mockSession;
    }

    // ✅ Тесты для handleTextMessage

    @Test
    void handleTextMessage_ShouldProcessPrivateMessageSuccessfully() throws Exception {
        // Arrange
        WebSocketSession session = createMockSession(TEST_USER_ID);
        String clientMessage = "test message";
        String kafkaJson = "kafka json";
        String successResponse = "success response";
        MessageEventDto event = new MessageEventDto();
        event.setType(MessageType.PRIVATE);
        event.setSenderId(TEST_USER_ID);
        event.setRecipientId(456L);
        event.setText("Hello");

        when(messageConverter.createMessageEvent(clientMessage, TEST_USER_ID)).thenReturn(event);
        when(messageConverter.convertToJson(event)).thenReturn(kafkaJson);
        when(messageConverter.createSuccessResponse()).thenReturn(successResponse);

        TextMessage message = new TextMessage(clientMessage);

        // Act
        chatHandler.handleTextMessage(session, message);

        // Assert
        verify(messageConverter).createMessageEvent(clientMessage, TEST_USER_ID);
        verify(messageConverter).convertToJson(event);
        verify(kafkaTemplate).send(eq("private-messages"), eq(kafkaJson));
        verify(session).sendMessage(textMessageCaptor.capture());
        assertEquals(successResponse, textMessageCaptor.getValue().getPayload());
    }

    @Test
    void handleTextMessage_ShouldProcessGroupMessageSuccessfully() throws Exception {
        // Arrange
        WebSocketSession session = createMockSession(TEST_USER_ID);
        String clientMessage = "test group message";
        String kafkaJson = "kafka json";
        String successResponse = "success response";
        MessageEventDto event = new MessageEventDto();
        event.setType(MessageType.GROUP);
        event.setSenderId(TEST_USER_ID);
        event.setChatId(789L);
        event.setText("Hello group");

        when(messageConverter.createMessageEvent(clientMessage, TEST_USER_ID)).thenReturn(event);
        when(messageConverter.convertToJson(event)).thenReturn(kafkaJson);
        when(messageConverter.createSuccessResponse()).thenReturn(successResponse);

        TextMessage message = new TextMessage(clientMessage);

        // Act
        chatHandler.handleTextMessage(session, message);

        // Assert
        verify(kafkaTemplate).send(eq("group-messages"), eq(kafkaJson));
        verify(session).sendMessage(textMessageCaptor.capture());
        assertEquals(successResponse, textMessageCaptor.getValue().getPayload());
    }

    @Test
    void handleTextMessage_ShouldSendValidationError_WhenMessageValidationFails() throws Exception {
        // Arrange
        WebSocketSession session = createMockSession(TEST_USER_ID);
        String clientMessage = "invalid message";
        String errorResponse = "validation error";

        when(messageConverter.createMessageEvent(clientMessage, TEST_USER_ID))
                .thenThrow(new MessageValidationException("Invalid message format"));
        when(messageConverter.createErrorResponse("VALIDATION_ERROR", "Invalid message format"))
                .thenReturn(errorResponse);

        TextMessage message = new TextMessage(clientMessage);

        // Act
        chatHandler.handleTextMessage(session, message);

        // Assert
        verify(messageConverter).createErrorResponse("VALIDATION_ERROR", "Invalid message format");
        verify(session).sendMessage(textMessageCaptor.capture());
        assertEquals(errorResponse, textMessageCaptor.getValue().getPayload());
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    void handleTextMessage_ShouldSendServerError_WhenUnexpectedExceptionOccurs() throws Exception {
        // Arrange
        WebSocketSession session = createMockSession(TEST_USER_ID);
        String clientMessage = "test message";
        String errorResponse = "server error";

        when(messageConverter.createMessageEvent(clientMessage, TEST_USER_ID))
                .thenThrow(new RuntimeException("Database connection failed"));
        when(messageConverter.createErrorResponse("SERVER_ERROR", "Внутренняя ошибка сервера"))
                .thenReturn(errorResponse);

        TextMessage message = new TextMessage(clientMessage);

        // Act
        chatHandler.handleTextMessage(session, message);

        // Assert
        verify(messageConverter).createErrorResponse("SERVER_ERROR", "Внутренняя ошибка сервера");
        verify(session).sendMessage(textMessageCaptor.capture());
        assertEquals(errorResponse, textMessageCaptor.getValue().getPayload());
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    // ✅ Тесты для afterConnectionEstablished

    @Test
    void afterConnectionEstablished_ShouldAddSessionAndSetLimits_WhenValidConnection() throws Exception {
        // Arrange
        WebSocketSession session = createMockSession(TEST_USER_ID);

        // Act
        chatHandler.afterConnectionEstablished(session);

        // Assert
        assertTrue(chatHandler.isUserOnline(TEST_USER_ID));
        verify(session).setBinaryMessageSizeLimit(1024 * 1024);
        verify(session).setTextMessageSizeLimit(1024 * 1024);
    }

    @Test
    void afterConnectionEstablished_ShouldHandleException_WhenInvalidUserId() throws Exception {
        // Arrange
        WebSocketSession session = mock(WebSocketSession.class);
        URI invalidUri = new URI("ws://localhost:8080/chat"); // без id параметра
        when(session.getUri()).thenReturn(invalidUri);

        // Act & Assert - не должно быть исключения
        assertDoesNotThrow(() -> chatHandler.afterConnectionEstablished(session));

        // Assert
        verify(session, never()).setBinaryMessageSizeLimit(anyInt());
        verify(session, never()).setTextMessageSizeLimit(anyInt());
    }

    // ✅ Тесты для afterConnectionClosed

    @Test
    void afterConnectionClosed_ShouldRemoveSession_WhenConnectionCloses() throws Exception {
        // Arrange
        WebSocketSession session = createMockSession(TEST_USER_ID);
        chatHandler.afterConnectionEstablished(session);
        assertTrue(chatHandler.isUserOnline(TEST_USER_ID));

        // Act
        chatHandler.afterConnectionClosed(session, CloseStatus.NORMAL);

        // Assert
        assertFalse(chatHandler.isUserOnline(TEST_USER_ID));
    }

    @Test
    void afterConnectionClosed_ShouldThrowException_WhenInvalidUrl() throws Exception {
        // Arrange
        WebSocketSession session = mock(WebSocketSession.class);
        URI invalidUri = new URI("ws://localhost:8080/chat"); // без id параметра
        when(session.getUri()).thenReturn(invalidUri);

        // Act & Assert - ожидаем исключение, так как метод бросает его при невалидном URL
        assertThrows(RuntimeException.class, () -> chatHandler.afterConnectionClosed(session, CloseStatus.NORMAL));
    }

    // ✅ Тесты для handleTransportError

    @Test
    void handleTransportError_ShouldCloseUserSession_WhenTransportErrorOccurs() throws Exception {
        // Arrange
        WebSocketSession session = createMockSession(TEST_USER_ID);
        chatHandler.afterConnectionEstablished(session);
        assertTrue(chatHandler.isUserOnline(TEST_USER_ID));

        Throwable transportError = new IOException("Network error");

        // Act
        chatHandler.handleTransportError(session, transportError);

        // Assert
        assertFalse(chatHandler.isUserOnline(TEST_USER_ID));
    }

    // ✅ Тесты для sendToUser

    @Test
    void sendToUser_ShouldSendMessage_WhenUserIsOnline() throws Exception {
        // Arrange
        WebSocketSession session = createMockSession(TEST_USER_ID);
        chatHandler.afterConnectionEstablished(session);
        String payload = "test message";

        // Act
        chatHandler.sendToUser(TEST_USER_ID, payload);

        // Assert
        verify(session).sendMessage(textMessageCaptor.capture());
        assertEquals(payload, textMessageCaptor.getValue().getPayload());
    }

    @Test
    void sendToUser_ShouldNotSendMessage_WhenUserIsOffline() throws IOException {
        // Act
        chatHandler.sendToUser(999L, "test message");

        // Assert - проверяем что НЕ было вызовов WebSocket
        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void sendToUser_ShouldRemoveSession_WhenSendFails() throws Exception {
        // Arrange
        WebSocketSession session = createMockSession(TEST_USER_ID);
        chatHandler.afterConnectionEstablished(session);
        String payload = "test message";

        doThrow(new IOException("Network error"))
                .when(session).sendMessage(any(TextMessage.class));

        // Act
        chatHandler.sendToUser(TEST_USER_ID, payload);

        // Assert
        assertFalse(chatHandler.isUserOnline(TEST_USER_ID));
    }

    // ✅ Тесты для sendToUser с объектом

    @Test
    void sendToUserWithObject_ShouldSerializeAndSend() throws Exception {
        // Arrange
        WebSocketSession session = createMockSession(TEST_USER_ID);
        chatHandler.afterConnectionEstablished(session);
        TestMessage testMessage = new TestMessage("Hello", "World");
        String serializedMessage = "{\"field1\":\"Hello\",\"field2\":\"World\"}";

        when(objectMapper.writeValueAsString(testMessage)).thenReturn(serializedMessage);

        // Act
        chatHandler.sendToUser(TEST_USER_ID, testMessage);

        // Assert
        verify(objectMapper).writeValueAsString(testMessage);
        verify(session).sendMessage(textMessageCaptor.capture());
        assertEquals(serializedMessage, textMessageCaptor.getValue().getPayload());
    }

    @Test
    void sendToUserWithObject_ShouldHandleSerializationError() throws Exception {
        // Arrange
        WebSocketSession session = createMockSession(TEST_USER_ID);
        chatHandler.afterConnectionEstablished(session);
        TestMessage testMessage = new TestMessage("Hello", "World");

        when(objectMapper.writeValueAsString(testMessage))
                .thenThrow(new RuntimeException("Serialization error"));

        // Act - не должно быть исключения
        assertDoesNotThrow(() -> chatHandler.sendToUser(TEST_USER_ID, testMessage));

        // Assert
        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    // ✅ Тесты для broadcastToChat

    @Test
    void broadcastToChat_ShouldSendToAllOnlineMembers() throws Exception {
        // Arrange
        Long chatId = 1L;
        List<Long> members = List.of(123L, 456L, 789L);
        String payload = "broadcast message";

        // Создаем сессии для онлайн пользователей
        WebSocketSession session1 = createMockSession(123L);
        WebSocketSession session2 = createMockSession(456L);

        when(chatMemberRepository.findUserIdsByChatId(chatId)).thenReturn(members);

        // Добавляем сессии
        chatHandler.afterConnectionEstablished(session1); // пользователь 123 онлайн
        chatHandler.afterConnectionEstablished(session2); // пользователь 456 онлайн
        // пользователь 789 оффлайн

        // Act
        chatHandler.broadcastToChat(chatId, payload);

        // Assert
        verify(session1).sendMessage(textMessageCaptor.capture());
        verify(session2).sendMessage(textMessageCaptor.capture());

        assertEquals(2, textMessageCaptor.getAllValues().size());
        textMessageCaptor.getAllValues().forEach(msg ->
                assertEquals(payload, msg.getPayload())
        );
    }

    @Test
    void broadcastToChat_ShouldHandleEmptyChat() {
        // Arrange
        Long chatId = 1L;
        String payload = "broadcast message";

        when(chatMemberRepository.findUserIdsByChatId(chatId)).thenReturn(List.of());

        // Act & Assert - не должно быть исключения
        assertDoesNotThrow(() -> chatHandler.broadcastToChat(chatId, payload));
    }

    // ✅ Тесты для isUserOnline

    @Test
    void isUserOnline_ShouldReturnTrue_WhenUserHasActiveSession() throws Exception {
        // Arrange
        WebSocketSession session = createMockSession(TEST_USER_ID);
        chatHandler.afterConnectionEstablished(session);

        // Act & Assert
        assertTrue(chatHandler.isUserOnline(TEST_USER_ID));
    }

    @Test
    void isUserOnline_ShouldReturnFalse_WhenUserNotInSessions() {
        // Act & Assert
        assertFalse(chatHandler.isUserOnline(999L));
    }

    @Test
    void isUserOnline_ShouldReturnFalse_WhenSessionIsClosed() throws Exception {
        // Arrange
        WebSocketSession session = createMockSession(TEST_USER_ID);
        chatHandler.afterConnectionEstablished(session);
        when(session.isOpen()).thenReturn(false); // сессия закрыта

        // Act & Assert
        assertFalse(chatHandler.isUserOnline(TEST_USER_ID));
    }

    // Вспомогательный класс для тестирования сериализации
    private static class TestMessage {
        private String field1;
        private String field2;

        public TestMessage(String field1, String field2) {
            this.field1 = field1;
            this.field2 = field2;
        }

        public String getField1() { return field1; }
        public String getField2() { return field2; }
    }
}