package com.example.kwizi.websocket;

import com.example.kwizi.DTO.internal.MessageEventDto;
import com.example.kwizi.enums.MessageType;
import com.example.kwizi.exception.MessageValidationException;
import com.example.kwizi.repository.ChatMemberRepository;
import com.example.kwizi.util.MessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
@DisplayName("UniversalChatHandler тесты")
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

    private WebSocketSession createMockSession(Long userId) throws Exception {
        WebSocketSession mockSession = mock(WebSocketSession.class);
        URI uri = new URI("ws://localhost:8080/chat?id=" + userId);
        when(mockSession.getUri()).thenReturn(uri);
        when(mockSession.isOpen()).thenReturn(true);
        when(mockSession.getId()).thenReturn("session-" + userId);
        return mockSession;
    }

    @Nested
    @DisplayName("Обработка текстовых сообщений")
    class HandleTextMessageTests {

        @Test
        @DisplayName("✅ Обрабатывает личное сообщение и отправляет в Kafka")
        void handleTextMessage_ShouldProcessPrivateMessageSuccessfully() throws Exception {
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

            chatHandler.handleTextMessage(session, message);

            verify(messageConverter).createMessageEvent(clientMessage, TEST_USER_ID);
            verify(messageConverter).convertToJson(event);
            verify(kafkaTemplate).send(eq("private-messages"), eq(kafkaJson));
            verify(session).sendMessage(textMessageCaptor.capture());
            assertThat(textMessageCaptor.getValue().getPayload()).isEqualTo(successResponse);
        }

        @Test
        @DisplayName("✅ Обрабатывает групповое сообщение и отправляет в Kafka")
        void handleTextMessage_ShouldProcessGroupMessageSuccessfully() throws Exception {
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

            chatHandler.handleTextMessage(session, message);

            verify(kafkaTemplate).send(eq("group-messages"), eq(kafkaJson));
            verify(session).sendMessage(textMessageCaptor.capture());
            assertThat(textMessageCaptor.getValue().getPayload()).isEqualTo(successResponse);
        }

        @Test
        @DisplayName("❌ Отправляет ошибку валидации при невалидном сообщении")
        void handleTextMessage_ShouldSendValidationError_WhenMessageValidationFails() throws Exception {
            WebSocketSession session = createMockSession(TEST_USER_ID);
            String clientMessage = "invalid message";
            String errorResponse = "validation error";

            when(messageConverter.createMessageEvent(clientMessage, TEST_USER_ID))
                    .thenThrow(new MessageValidationException("Invalid message format"));
            when(messageConverter.createErrorResponse("VALIDATION_ERROR", "Invalid message format"))
                    .thenReturn(errorResponse);
            TextMessage message = new TextMessage(clientMessage);

            chatHandler.handleTextMessage(session, message);

            verify(messageConverter).createErrorResponse("VALIDATION_ERROR", "Invalid message format");
            verify(session).sendMessage(textMessageCaptor.capture());
            assertThat(textMessageCaptor.getValue().getPayload()).isEqualTo(errorResponse);
            verify(kafkaTemplate, never()).send(anyString(), anyString());
        }

        @Test
        @DisplayName("❌ Отправляет ошибку сервера при непредвиденном исключении")
        void handleTextMessage_ShouldSendServerError_WhenUnexpectedExceptionOccurs() throws Exception {
            WebSocketSession session = createMockSession(TEST_USER_ID);
            String clientMessage = "test message";
            String errorResponse = "server error";

            when(messageConverter.createMessageEvent(clientMessage, TEST_USER_ID))
                    .thenThrow(new RuntimeException("Database connection failed"));
            when(messageConverter.createErrorResponse("SERVER_ERROR", "Внутренняя ошибка сервера"))
                    .thenReturn(errorResponse);
            TextMessage message = new TextMessage(clientMessage);

            chatHandler.handleTextMessage(session, message);

            verify(messageConverter).createErrorResponse("SERVER_ERROR", "Внутренняя ошибка сервера");
            verify(session).sendMessage(textMessageCaptor.capture());
            assertThat(textMessageCaptor.getValue().getPayload()).isEqualTo(errorResponse);
            verify(kafkaTemplate, never()).send(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Управление подключениями")
    class ConnectionManagementTests {

        @Test
        @DisplayName("✅ Добавляет сессию и устанавливает лимиты при валидном подключении")
        void afterConnectionEstablished_ShouldAddSessionAndSetLimits_WhenValidConnection() throws Exception {
            WebSocketSession session = createMockSession(TEST_USER_ID);
            chatHandler.afterConnectionEstablished(session);
            assertThat(chatHandler.isUserOnline(TEST_USER_ID)).isTrue();
            verify(session).setBinaryMessageSizeLimit(1024 * 1024);
            verify(session).setTextMessageSizeLimit(1024 * 1024);
        }

        @Test
        @DisplayName("✅ Безопасно обрабатывает подключение без ID пользователя")
        void afterConnectionEstablished_ShouldHandleException_WhenInvalidUserId() throws Exception {
            WebSocketSession session = mock(WebSocketSession.class);
            URI invalidUri = new URI("ws://localhost:8080/chat");
            when(session.getUri()).thenReturn(invalidUri);
            assertThatCode(() -> chatHandler.afterConnectionEstablished(session)).doesNotThrowAnyException();
            verify(session, never()).setBinaryMessageSizeLimit(anyInt());
            verify(session, never()).setTextMessageSizeLimit(anyInt());
        }

        @Test
        @DisplayName("✅ Удаляет сессию при закрытии соединения")
        void afterConnectionClosed_ShouldRemoveSession_WhenConnectionCloses() throws Exception {
            WebSocketSession session = createMockSession(TEST_USER_ID);
            chatHandler.afterConnectionEstablished(session);
            assertThat(chatHandler.isUserOnline(TEST_USER_ID)).isTrue();
            chatHandler.afterConnectionClosed(session, CloseStatus.NORMAL);
            assertThat(chatHandler.isUserOnline(TEST_USER_ID)).isFalse();
        }

        @Test
        @DisplayName("❌ Выбрасывает исключение при закрытии сессии с невалидным URI")
        void afterConnectionClosed_ShouldThrowException_WhenInvalidUrl() throws Exception {
            WebSocketSession session = mock(WebSocketSession.class);
            URI invalidUri = new URI("ws://localhost:8080/chat");
            when(session.getUri()).thenReturn(invalidUri);
            assertThatThrownBy(() -> chatHandler.afterConnectionClosed(session, CloseStatus.NORMAL))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("✅ Закрывает сессию пользователя при транспортной ошибке")
        void handleTransportError_ShouldCloseUserSession_WhenTransportErrorOccurs() throws Exception {
            WebSocketSession session = createMockSession(TEST_USER_ID);
            chatHandler.afterConnectionEstablished(session);
            assertThat(chatHandler.isUserOnline(TEST_USER_ID)).isTrue();
            chatHandler.handleTransportError(session, new IOException("Network error"));
            assertThat(chatHandler.isUserOnline(TEST_USER_ID)).isFalse();
        }
    }

    @Nested
    @DisplayName("Отправка сообщений")
    class SendMessageTests {

        @Test
        @DisplayName("✅ Отправляет сообщение онлайн-пользователю")
        void sendToUser_ShouldSendMessage_WhenUserIsOnline() throws Exception {
            WebSocketSession session = createMockSession(TEST_USER_ID);
            chatHandler.afterConnectionEstablished(session);
            String payload = "test message";
            chatHandler.sendToUser(TEST_USER_ID, payload);
            verify(session).sendMessage(textMessageCaptor.capture());
            assertThat(textMessageCaptor.getValue().getPayload()).isEqualTo(payload);
        }

        @Test
        @DisplayName("❌ Не отправляет сообщение оффлайн-пользователю")
        void sendToUser_ShouldNotSendMessage_WhenUserIsOffline() throws IOException {
            chatHandler.sendToUser(999L, "test message");
            verify(session, never()).sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("✅ Удаляет сессию при ошибке отправки")
        void sendToUser_ShouldRemoveSession_WhenSendFails() throws Exception {
            WebSocketSession session = createMockSession(TEST_USER_ID);
            chatHandler.afterConnectionEstablished(session);
            String payload = "test message";
            doThrow(new IOException("Network error")).when(session).sendMessage(any(TextMessage.class));
            chatHandler.sendToUser(TEST_USER_ID, payload);
            assertThat(chatHandler.isUserOnline(TEST_USER_ID)).isFalse();
        }

        @Test
        @DisplayName("✅ Сериализует и отправляет объект онлайн-пользователю")
        void sendToUserWithObject_ShouldSerializeAndSend() throws Exception {
            WebSocketSession session = createMockSession(TEST_USER_ID);
            chatHandler.afterConnectionEstablished(session);
            TestMessage testMessage = new TestMessage("Hello", "World");
            String serializedMessage = "{\"field1\":\"Hello\",\"field2\":\"World\"}";
            when(objectMapper.writeValueAsString(testMessage)).thenReturn(serializedMessage);
            chatHandler.sendToUser(TEST_USER_ID, testMessage);
            verify(objectMapper).writeValueAsString(testMessage);
            verify(session).sendMessage(textMessageCaptor.capture());
            assertThat(textMessageCaptor.getValue().getPayload()).isEqualTo(serializedMessage);
        }

        @Test
        @DisplayName("✅ Безопасно обрабатывает ошибку сериализации")
        void sendToUserWithObject_ShouldHandleSerializationError() throws Exception {
            WebSocketSession session = createMockSession(TEST_USER_ID);
            chatHandler.afterConnectionEstablished(session);
            TestMessage testMessage = new TestMessage("Hello", "World");
            when(objectMapper.writeValueAsString(testMessage)).thenThrow(new RuntimeException("Serialization error"));
            assertThatCode(() -> chatHandler.sendToUser(TEST_USER_ID, testMessage)).doesNotThrowAnyException();
            verify(session, never()).sendMessage(any(TextMessage.class));
        }
    }

    @Nested
    @DisplayName("Рассылка сообщений в чат")
    class BroadcastTests {

        @Test
        @DisplayName("✅ Отправляет сообщение всем онлайн-участникам чата")
        void broadcastToChat_ShouldSendToAllOnlineMembers() throws Exception {
            Long chatId = 1L;
            List<Long> members = List.of(123L, 456L, 789L);
            String payload = "broadcast message";
            WebSocketSession session1 = createMockSession(123L);
            WebSocketSession session2 = createMockSession(456L);
            when(chatMemberRepository.findUserIdsByChatId(chatId)).thenReturn(members);
            chatHandler.afterConnectionEstablished(session1);
            chatHandler.afterConnectionEstablished(session2);
            chatHandler.broadcastToChat(chatId, payload);
            verify(session1).sendMessage(any(TextMessage.class));
            verify(session2).sendMessage(any(TextMessage.class));
            // Проверка количества вызовов через captor не обязательна при strict verification
        }

        @Test
        @DisplayName("✅ Безопасно обрабатывает рассылку в пустой чат")
        void broadcastToChat_ShouldHandleEmptyChat() {
            Long chatId = 1L;
            String payload = "broadcast message";
            when(chatMemberRepository.findUserIdsByChatId(chatId)).thenReturn(List.of());
            assertThatCode(() -> chatHandler.broadcastToChat(chatId, payload)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Проверка онлайна пользователя")
    class IsUserOnlineTests {

        @Test
        @DisplayName("✅ Возвращает true, если у пользователя есть активная сессия")
        void isUserOnline_ShouldReturnTrue_WhenUserHasActiveSession() throws Exception {
            WebSocketSession session = createMockSession(TEST_USER_ID);
            chatHandler.afterConnectionEstablished(session);
            assertThat(chatHandler.isUserOnline(TEST_USER_ID)).isTrue();
        }

        @Test
        @DisplayName("✅ Возвращает false, если пользователь не подключен")
        void isUserOnline_ShouldReturnFalse_WhenUserNotInSessions() {
            assertThat(chatHandler.isUserOnline(999L)).isFalse();
        }

        @Test
        @DisplayName("✅ Возвращает false, если сессия закрыта")
        void isUserOnline_ShouldReturnFalse_WhenSessionIsClosed() throws Exception {
            WebSocketSession session = createMockSession(TEST_USER_ID);
            chatHandler.afterConnectionEstablished(session);
            when(session.isOpen()).thenReturn(false);
            assertThat(chatHandler.isUserOnline(TEST_USER_ID)).isFalse();
        }
    }

    private static class TestMessage {
        private String field1;
        private String field2;

        public TestMessage(String field1, String field2) {
            this.field1 = field1;
            this.field2 = field2;
        }

        public String getField1() {
            return field1;
        }

        public String getField2() {
            return field2;
        }
    }
}