package com.example.kwizi.websocket;

import com.example.kwizi.DTO.internal.MessageEventDto;
import com.example.kwizi.enums.ChatType;
import com.example.kwizi.enums.MessageType;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("UniversalChatHandler тесты")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UniversalChatHandlerTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ChatMemberRepository chatMemberRepository;

    @Mock
    private MessageConverter messageConverter;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private WebSocketSession session;

    @Captor
    private ArgumentCaptor<TextMessage> textMessageCaptor;

    @InjectMocks
    private UniversalChatHandler chatHandler;

    private static final Long TEST_USER_ID = 123L;
    private static final String VALID_TOKEN = "valid.jwt.token";

    private WebSocketSession createMockSession(String token) throws Exception {
        WebSocketSession mockSession = mock(WebSocketSession.class);
        URI uri = new URI("ws://localhost:8080/ws?token=" + token);
        when(mockSession.getUri()).thenReturn(uri);
        when(mockSession.isOpen()).thenReturn(true);
        lenient().when(mockSession.getId()).thenReturn("test-session-id");
        return mockSession;
    }

    @Nested
    @DisplayName("Обработка текстовых сообщений")
    class HandleTextMessageTests {

        @Test
        @DisplayName("Обрабатывает личное сообщение и отправляет в Kafka")
        void handleTextMessage_ShouldProcessPrivateMessageSuccessfully() throws Exception {
            when(jwtUtils.isTokenRevoked(VALID_TOKEN)).thenReturn(false);
            when(jwtUtils.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);

            WebSocketSession session = createMockSession(VALID_TOKEN);
            String clientMessage = "test message";
            String kafkaJson = "kafka json";
            String successResponse = "success response";
            MessageEventDto event = new MessageEventDto();
            event.setType(MessageType.PRIVATE);
            event.setSenderId(TEST_USER_ID);
            event.setRecipientId(456L);
            event.setText("Hello");

            when(messageConverter.createMessageEvent(clientMessage, TEST_USER_ID)).thenReturn(event);
            when(userRepository.existsById(456L)).thenReturn(true);
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
        @DisplayName("Обрабатывает групповое сообщение и отправляет в Kafka")
        void handleTextMessage_ShouldProcessGroupMessageSuccessfully() throws Exception {
            when(jwtUtils.isTokenRevoked(VALID_TOKEN)).thenReturn(false);
            when(jwtUtils.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);

            WebSocketSession session = createMockSession(VALID_TOKEN);
            String clientMessage = "test group message";
            String kafkaJson = "kafka json";
            String successResponse = "success response";
            MessageEventDto event = new MessageEventDto();
            event.setType(MessageType.GROUP);
            event.setSenderId(TEST_USER_ID);
            event.setChatId(789L);
            event.setText("Hello group");

            Chat chat = new Chat();
            chat.setId(789L);
            chat.setChatType(ChatType.GROUP);

            when(messageConverter.createMessageEvent(clientMessage, TEST_USER_ID)).thenReturn(event);
            when(chatRepository.existsById(789L)).thenReturn(true);
            when(chatRepository.findById(789L)).thenReturn(Optional.of(chat));
            when(chatMemberRepository.existsByChatIdAndUserId(789L, TEST_USER_ID)).thenReturn(true);
            when(messageConverter.convertToJson(event)).thenReturn(kafkaJson);
            when(messageConverter.createSuccessResponse()).thenReturn(successResponse);
            TextMessage message = new TextMessage(clientMessage);

            chatHandler.handleTextMessage(session, message);

            verify(kafkaTemplate).send(eq("group-messages"), eq(kafkaJson));
            verify(session).sendMessage(textMessageCaptor.capture());
            assertThat(textMessageCaptor.getValue().getPayload()).isEqualTo(successResponse);
        }

        @Test
        @DisplayName("Отправляет ошибку валидации при невалидном сообщении")
        void handleTextMessage_ShouldSendValidationError_WhenMessageValidationFails() throws Exception {
            when(jwtUtils.isTokenRevoked(VALID_TOKEN)).thenReturn(false);
            when(jwtUtils.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);

            WebSocketSession session = createMockSession(VALID_TOKEN);
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
        @DisplayName("Отправляет ошибку USER_NOT_FOUND для несуществующего получателя")
        void handleTextMessage_ShouldSendUserNotFoundError_WhenRecipientNotFound() throws Exception {
            when(jwtUtils.isTokenRevoked(VALID_TOKEN)).thenReturn(false);
            when(jwtUtils.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);

            WebSocketSession session = createMockSession(VALID_TOKEN);
            String clientMessage = "private message";
            MessageEventDto event = new MessageEventDto();
            event.setType(MessageType.PRIVATE);
            event.setSenderId(TEST_USER_ID);
            event.setRecipientId(456L);
            event.setText("Hello");

            String errorResponse = "user not found error";

            when(messageConverter.createMessageEvent(clientMessage, TEST_USER_ID)).thenReturn(event);
            when(userRepository.existsById(456L)).thenReturn(false);
            when(messageConverter.createErrorResponse("USER_NOT_FOUND", "Получатель с ID 456 не найден"))
                    .thenReturn(errorResponse);
            TextMessage message = new TextMessage(clientMessage);

            chatHandler.handleTextMessage(session, message);

            verify(messageConverter).createErrorResponse("USER_NOT_FOUND", "Получатель с ID 456 не найден");
            verify(session).sendMessage(textMessageCaptor.capture());
            assertThat(textMessageCaptor.getValue().getPayload()).isEqualTo(errorResponse);
            verify(kafkaTemplate, never()).send(anyString(), anyString());
        }

        @Test
        @DisplayName("Отправляет ошибку CHAT_NOT_FOUND для несуществующего чата")
        void handleTextMessage_ShouldSendChatNotFoundError_WhenChatDoesNotExist() throws Exception {
            when(jwtUtils.isTokenRevoked(VALID_TOKEN)).thenReturn(false);
            when(jwtUtils.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);

            WebSocketSession session = createMockSession(VALID_TOKEN);
            String clientMessage = "group message";
            MessageEventDto event = new MessageEventDto();
            event.setType(MessageType.GROUP);
            event.setSenderId(TEST_USER_ID);
            event.setChatId(789L);
            event.setText("Hello group");

            String errorResponse = "chat not found error";

            when(messageConverter.createMessageEvent(clientMessage, TEST_USER_ID)).thenReturn(event);
            when(chatRepository.existsById(789L)).thenReturn(false);
            when(messageConverter.createErrorResponse("CHAT_NOT_FOUND", "Чат с ID 789 не найден"))
                    .thenReturn(errorResponse);
            TextMessage message = new TextMessage(clientMessage);

            chatHandler.handleTextMessage(session, message);

            verify(messageConverter).createErrorResponse("CHAT_NOT_FOUND", "Чат с ID 789 не найден");
            verify(session).sendMessage(textMessageCaptor.capture());
            assertThat(textMessageCaptor.getValue().getPayload()).isEqualTo(errorResponse);
            verify(kafkaTemplate, never()).send(anyString(), anyString());
        }

        @Test
        @DisplayName("Отправляет ошибку ACCESS_DENIED при отсутствии доступа к чату")
        void handleTextMessage_ShouldSendAccessDeniedError_WhenUserNotInChat() throws Exception {
            when(jwtUtils.isTokenRevoked(VALID_TOKEN)).thenReturn(false);
            when(jwtUtils.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);

            WebSocketSession session = createMockSession(VALID_TOKEN);
            String clientMessage = "group message";
            MessageEventDto event = new MessageEventDto();
            event.setType(MessageType.GROUP);
            event.setSenderId(TEST_USER_ID);
            event.setChatId(789L);
            event.setText("Hello group");

            Chat chat = new Chat();
            chat.setId(789L);
            chat.setChatType(ChatType.GROUP);

            String errorResponse = "access denied error";

            when(messageConverter.createMessageEvent(clientMessage, TEST_USER_ID)).thenReturn(event);
            when(chatRepository.existsById(789L)).thenReturn(true);
            when(chatRepository.findById(789L)).thenReturn(Optional.of(chat));
            when(chatMemberRepository.existsByChatIdAndUserId(789L, TEST_USER_ID)).thenReturn(false);
            when(messageConverter.createErrorResponse("ACCESS_DENIED", "Вы не являетесь участником этого чата"))
                    .thenReturn(errorResponse);
            TextMessage message = new TextMessage(clientMessage);

            chatHandler.handleTextMessage(session, message);

            verify(messageConverter).createErrorResponse("ACCESS_DENIED", "Вы не являетесь участником этого чата");
            verify(session).sendMessage(textMessageCaptor.capture());
            assertThat(textMessageCaptor.getValue().getPayload()).isEqualTo(errorResponse);
            verify(kafkaTemplate, never()).send(anyString(), anyString());
        }

        @Test
        @DisplayName("Отправляет ошибку сервера при непредвиденном исключении")
        void handleTextMessage_ShouldSendServerError_WhenUnexpectedExceptionOccurs() throws Exception {
            when(jwtUtils.isTokenRevoked(VALID_TOKEN)).thenReturn(false);
            when(jwtUtils.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);

            WebSocketSession session = createMockSession(VALID_TOKEN);
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
    @DisplayName("Валидация событий")
    class EventValidationTests {

        @Test
        @DisplayName("Проходит валидацию для приватного сообщения с существующим получателем")
        void validateEventBeforeSending_ShouldPass_ForPrivateMessageWithExistingRecipient() {
            MessageEventDto event = new MessageEventDto();
            event.setType(MessageType.PRIVATE);
            event.setSenderId(123L);
            event.setRecipientId(456L);

            when(userRepository.existsById(456L)).thenReturn(true);

            assertThatCode(() -> ReflectionTestUtils.invokeMethod(chatHandler,
                    "validateEventBeforeSending", event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Бросает UserNotFoundException для несуществующего получателя")
        void validateEventBeforeSending_ShouldThrowUserNotFoundException_ForNonExistingRecipient() {
            MessageEventDto event = new MessageEventDto();
            event.setType(MessageType.PRIVATE);
            event.setSenderId(123L);
            event.setRecipientId(456L);

            when(userRepository.existsById(456L)).thenReturn(false);

            assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(chatHandler,
                    "validateEventBeforeSending", event))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("Получатель с ID 456 не найден");
        }

        @Test
        @DisplayName("Бросает MessageValidationException при отсутствии chatId для группового сообщения")
        void validateEventBeforeSending_ShouldThrowValidationException_ForGroupMessageWithoutChatId() {
            MessageEventDto event = new MessageEventDto();
            event.setType(MessageType.GROUP);
            event.setSenderId(123L);
            event.setChatId(null);

            assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(chatHandler,
                    "validateEventBeforeSending", event))
                    .isInstanceOf(MessageValidationException.class)
                    .hasMessageContaining("Для группового сообщения обязателен chatId");
        }

        @Test
        @DisplayName("Бросает ChatNotFoundException для несуществующего чата")
        void validateEventBeforeSending_ShouldThrowChatNotFoundException_ForNonExistingChat() {
            MessageEventDto event = new MessageEventDto();
            event.setType(MessageType.GROUP);
            event.setSenderId(123L);
            event.setChatId(789L);

            when(chatRepository.existsById(789L)).thenReturn(false);

            assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(chatHandler,
                    "validateEventBeforeSending", event))
                    .isInstanceOf(ChatNotFoundException.class)
                    .hasMessageContaining("Чат с ID 789 не найден");
        }

        @Test
        @DisplayName("Бросает MessageValidationException при отправке группового сообщения в приватный чат")
        void validateEventBeforeSending_ShouldThrowValidationException_ForGroupMessageInPrivateChat() {
            MessageEventDto event = new MessageEventDto();
            event.setType(MessageType.GROUP);
            event.setSenderId(123L);
            event.setChatId(789L);

            Chat chat = new Chat();
            chat.setId(789L);
            chat.setChatType(ChatType.PRIVATE);

            when(chatRepository.existsById(789L)).thenReturn(true);
            when(chatRepository.findById(789L)).thenReturn(Optional.of(chat));
            when(chatMemberRepository.existsByChatIdAndUserId(789L, 123L)).thenReturn(true);

            assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(chatHandler,
                    "validateEventBeforeSending", event))
                    .isInstanceOf(MessageValidationException.class)
                    .hasMessageContaining("Нельзя отправить групповое сообщение в приватный чат");
        }

        @Test
        @DisplayName("Бросает AccessDeniedException при отправке в чужой чат")
        void validateEventBeforeSending_ShouldThrowAccessDeniedException_WhenUserNotInChat() {
            MessageEventDto event = new MessageEventDto();
            event.setType(MessageType.GROUP);
            event.setSenderId(123L);
            event.setChatId(789L);

            Chat chat = new Chat();
            chat.setId(789L);
            chat.setChatType(ChatType.GROUP);

            when(chatRepository.existsById(789L)).thenReturn(true);
            when(chatRepository.findById(789L)).thenReturn(Optional.of(chat));
            when(chatMemberRepository.existsByChatIdAndUserId(789L, 123L)).thenReturn(false);

            assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(chatHandler,
                    "validateEventBeforeSending", event))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Вы не являетесь участником этого чата");
        }
    }

    @Nested
    @DisplayName("Управление подключениями")
    class ConnectionManagementTests {

        @Test
        @DisplayName("Добавляет сессию и устанавливает лимиты при валидном подключении")
        void afterConnectionEstablished_ShouldAddSessionAndSetLimits_WhenValidConnection() throws Exception {
            when(jwtUtils.isTokenRevoked(VALID_TOKEN)).thenReturn(false);
            when(jwtUtils.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);

            WebSocketSession session = createMockSession(VALID_TOKEN);
            chatHandler.afterConnectionEstablished(session);
            assertThat(chatHandler.isUserOnline(TEST_USER_ID)).isTrue();
            verify(session).setBinaryMessageSizeLimit(1024 * 1024);
            verify(session).setTextMessageSizeLimit(1024 * 1024);
        }

        @Test
        @DisplayName("Безопасно обрабатывает подключение без токена")
        void afterConnectionEstablished_ShouldHandleException_WhenNoToken() throws Exception {
            WebSocketSession session = mock(WebSocketSession.class);
            URI invalidUri = new URI("ws://localhost:8080/ws");
            when(session.getUri()).thenReturn(invalidUri);
            when(session.isOpen()).thenReturn(true);

            chatHandler.afterConnectionEstablished(session);

            verify(session).close(eq(CloseStatus.BAD_DATA));
            assertThat(chatHandler.isUserOnline(TEST_USER_ID)).isFalse();
        }

        @Test
        @DisplayName("Удаляет сессию при закрытии соединения")
        void afterConnectionClosed_ShouldRemoveSession_WhenConnectionCloses() throws Exception {
            when(jwtUtils.isTokenRevoked(VALID_TOKEN)).thenReturn(false);
            when(jwtUtils.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);

            WebSocketSession session = createMockSession(VALID_TOKEN);
            chatHandler.afterConnectionEstablished(session);
            assertThat(chatHandler.isUserOnline(TEST_USER_ID)).isTrue();
            chatHandler.afterConnectionClosed(session, CloseStatus.NORMAL);
            assertThat(chatHandler.isUserOnline(TEST_USER_ID)).isFalse();
        }

        @Test
        @DisplayName("Безопасно обрабатывает закрытие сессии без токена")
        void afterConnectionClosed_ShouldNotFail_WhenNoToken() throws Exception {
            WebSocketSession session = mock(WebSocketSession.class);
            URI invalidUri = new URI("ws://localhost:8080/ws");
            when(session.getUri()).thenReturn(invalidUri);

            assertThatCode(() -> chatHandler.afterConnectionClosed(session, CloseStatus.NORMAL))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Закрывает сессию пользователя при транспортной ошибке")
        void handleTransportError_ShouldCloseUserSession_WhenTransportErrorOccurs() throws Exception {
            when(jwtUtils.isTokenRevoked(VALID_TOKEN)).thenReturn(false);
            when(jwtUtils.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);

            WebSocketSession session = createMockSession(VALID_TOKEN);
            chatHandler.afterConnectionEstablished(session);
            assertThat(chatHandler.isUserOnline(TEST_USER_ID)).isTrue();
            chatHandler.handleTransportError(session, new IOException("Network error"));
            assertThat(chatHandler.isUserOnline(TEST_USER_ID)).isFalse();
        }
    }

    @Nested
    @DisplayName("Закрытие сессии пользователя")
    class CloseUserSessionTests {

        @Test
        @DisplayName("Закрывает сессию пользователя при logout")
        void closeUserSession_ShouldCloseSession_WhenUserHasActiveSession() throws Exception {
            when(jwtUtils.isTokenRevoked(VALID_TOKEN)).thenReturn(false);
            when(jwtUtils.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);

            WebSocketSession session = createMockSession(VALID_TOKEN);
            chatHandler.afterConnectionEstablished(session);
            assertThat(chatHandler.isUserOnline(TEST_USER_ID)).isTrue();

            chatHandler.closeUserSession(TEST_USER_ID);

            verify(session).close(eq(CloseStatus.GOING_AWAY));
            assertThat(chatHandler.isUserOnline(TEST_USER_ID)).isFalse();
        }

        @Test
        @DisplayName("Безопасно обрабатывает закрытие несуществующей сессии")
        void closeUserSession_ShouldHandleNonExistingSession() {
            assertThatCode(() -> chatHandler.closeUserSession(999L))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Отправка сообщений")
    class SendMessageTests {

        @Test
        @DisplayName("Отправляет сообщение онлайн-пользователю")
        void sendToUser_ShouldSendMessage_WhenUserIsOnline() throws Exception {
            when(jwtUtils.isTokenRevoked(VALID_TOKEN)).thenReturn(false);
            when(jwtUtils.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);

            WebSocketSession session = createMockSession(VALID_TOKEN);
            chatHandler.afterConnectionEstablished(session);
            String payload = "test message";
            chatHandler.sendToUser(TEST_USER_ID, payload);
            verify(session).sendMessage(textMessageCaptor.capture());
            assertThat(textMessageCaptor.getValue().getPayload()).isEqualTo(payload);
        }

        @Test
        @DisplayName("Не отправляет сообщение оффлайн-пользователю")
        void sendToUser_ShouldNotSendMessage_WhenUserIsOffline() {
            chatHandler.sendToUser(999L, "test message");
        }

        @Test
        @DisplayName("Удаляет сессию при ошибке отправки")
        void sendToUser_ShouldRemoveSession_WhenSendFails() throws Exception {
            when(jwtUtils.isTokenRevoked(VALID_TOKEN)).thenReturn(false);
            when(jwtUtils.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);

            WebSocketSession session = createMockSession(VALID_TOKEN);
            chatHandler.afterConnectionEstablished(session);
            String payload = "test message";
            doThrow(new IOException("Network error")).when(session).sendMessage(any(TextMessage.class));
            chatHandler.sendToUser(TEST_USER_ID, payload);
            assertThat(chatHandler.isUserOnline(TEST_USER_ID)).isFalse();
        }

        @Test
        @DisplayName("Сериализует и отправляет объект онлайн-пользователю")
        void sendToUserWithObject_ShouldSerializeAndSend() throws Exception {
            when(jwtUtils.isTokenRevoked(VALID_TOKEN)).thenReturn(false);
            when(jwtUtils.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);

            WebSocketSession session = createMockSession(VALID_TOKEN);
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
        @DisplayName("Безопасно обрабатывает ошибку сериализации")
        void sendToUserWithObject_ShouldHandleSerializationError() throws Exception {
            when(jwtUtils.isTokenRevoked(VALID_TOKEN)).thenReturn(false);
            when(jwtUtils.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);

            WebSocketSession session = createMockSession(VALID_TOKEN);
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
        @DisplayName("Отправляет сообщение всем онлайн-участникам чата")
        void broadcastToChat_ShouldSendToAllOnlineMembers() throws Exception {
            when(jwtUtils.isTokenRevoked(VALID_TOKEN)).thenReturn(false);
            when(jwtUtils.getUserIdFromToken(VALID_TOKEN)).thenReturn(123L);
            when(jwtUtils.getUserIdFromToken("token-456")).thenReturn(456L);

            Long chatId = 1L;
            List<Long> members = List.of(123L, 456L, 789L);
            String payload = "broadcast message";

            WebSocketSession session1 = createMockSession(VALID_TOKEN);
            WebSocketSession session2 = createMockSession("token-456");

            when(chatMemberRepository.findUserIdsByChatId(chatId)).thenReturn(members);
            chatHandler.afterConnectionEstablished(session1);
            chatHandler.afterConnectionEstablished(session2);

            chatHandler.broadcastToChat(chatId, payload);

            verify(session1).sendMessage(any(TextMessage.class));
            verify(session2).sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("Безопасно обрабатывает рассылку в пустой чат")
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
        @DisplayName("Возвращает true, если у пользователя есть активная сессия")
        void isUserOnline_ShouldReturnTrue_WhenUserHasActiveSession() throws Exception {
            when(jwtUtils.isTokenRevoked(VALID_TOKEN)).thenReturn(false);
            when(jwtUtils.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);

            WebSocketSession session = createMockSession(VALID_TOKEN);
            chatHandler.afterConnectionEstablished(session);
            assertThat(chatHandler.isUserOnline(TEST_USER_ID)).isTrue();
        }

        @Test
        @DisplayName("Возвращает false, если пользователь не подключен")
        void isUserOnline_ShouldReturnFalse_WhenUserNotInSessions() {
            assertThat(chatHandler.isUserOnline(999L)).isFalse();
        }

        @Test
        @DisplayName("Возвращает false, если сессия закрыта")
        void isUserOnline_ShouldReturnFalse_WhenSessionIsClosed() throws Exception {
            when(jwtUtils.isTokenRevoked(VALID_TOKEN)).thenReturn(false);
            when(jwtUtils.getUserIdFromToken(VALID_TOKEN)).thenReturn(TEST_USER_ID);

            WebSocketSession session = createMockSession(VALID_TOKEN);
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
    }
}