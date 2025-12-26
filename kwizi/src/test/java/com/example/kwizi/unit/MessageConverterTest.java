package com.example.kwizi.unit;

import com.example.kwizi.DTO.internal.MessageDto;
import com.example.kwizi.DTO.internal.MessageEventDto;
import com.example.kwizi.enums.MessageType;
import com.example.kwizi.model.Chat;
import com.example.kwizi.model.Message;
import com.example.kwizi.model.User;
import com.example.kwizi.util.MessageConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;


import static org.junit.jupiter.api.Assertions.*;


import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageConverterTest {

    @Mock
    private ObjectMapper objectMapper;

    private MessageConverter messageConverter;

    private Message testMessage;
    private MessageEventDto testEvent;
    private User testSender;
    private Chat testChat;

    @BeforeEach
    void setUp() {
        messageConverter = new MessageConverter(objectMapper);

        // Setup test data
        testSender = new User();
        testSender.setId(1L);
        testSender.setUsername("testuser");

        testChat = new Chat();
        testChat.setId(100L);

        testMessage = new Message();
        testMessage.setId(999L);
        testMessage.setText("Test message");
        testMessage.setCreatedAt(OffsetDateTime.now());
        testMessage.setSender(testSender);
        testMessage.setChat(testChat);

        testEvent = new MessageEventDto();
        testEvent.setType(MessageType.PRIVATE);
        testEvent.setSenderId(1L);
        testEvent.setRecipientId(2L);
        testEvent.setText("Hello");
        testEvent.setTimestamp(Instant.now());
    }

    @Nested
    @DisplayName("Конвертация JSON в MessageEventDto (convertToEvent)")
    class ConvertToEventTests {

        @Test
        void convertToEvent_ShouldReturnEvent_WhenValidJson() throws Exception {
            // Arrange
            String validJson = "{\"type\":\"PRIVATE\",\"senderId\":1,\"recipientId\":2,\"text\":\"Hello\"}";
            when(objectMapper.readValue(validJson, MessageEventDto.class)).thenReturn(testEvent);

            // Act
            MessageEventDto result = messageConverter.convertToEvent(validJson);

            // Assert
            assertNotNull(result);
            assertEquals(testEvent, result);
            verify(objectMapper).readValue(validJson, MessageEventDto.class);
        }

        @Test
        void convertToEvent_ShouldThrowRuntimeException_WhenInvalidJson() throws Exception {
            // Arrange
            String invalidJson = "invalid json";
            when(objectMapper.readValue(invalidJson, MessageEventDto.class))
                    .thenThrow(new JsonProcessingException("Invalid JSON") {});

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> messageConverter.convertToEvent(invalidJson));

            assertEquals("Неверный формат сообщения", exception.getMessage());
            assertNotNull(exception.getCause());
        }

        @Test
        void convertToEvent_ShouldHandleEmptyJson() throws Exception {
            // Arrange
            String emptyJson = "{}";
            MessageEventDto emptyEvent = new MessageEventDto();
            when(objectMapper.readValue(emptyJson, MessageEventDto.class)).thenReturn(emptyEvent);

            // Act
            MessageEventDto result = messageConverter.convertToEvent(emptyJson);

            // Assert
            assertNotNull(result);
            verify(objectMapper).readValue(emptyJson, MessageEventDto.class);
        }
    }

    @Nested
    @DisplayName("Сериализация MessageEventDto в JSON (convertToJson)")
    class ConvertToJsonTests {

        @Test
        void convertToJson_ShouldReturnJson_WhenValidEvent() throws Exception {
            // Arrange
            String expectedJson = "{\"type\":\"PRIVATE\",\"text\":\"Hello\"}";
            when(objectMapper.writeValueAsString(testEvent)).thenReturn(expectedJson);

            // Act
            String result = messageConverter.convertToJson(testEvent);

            // Assert
            assertEquals(expectedJson, result);
            verify(objectMapper).writeValueAsString(testEvent);
        }

        @Test
        void convertToJson_ShouldThrowRuntimeException_WhenSerializationFails() throws Exception {
            // Arrange
            when(objectMapper.writeValueAsString(testEvent))
                    .thenThrow(new JsonProcessingException("Serialization failed") {});

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> messageConverter.convertToJson(testEvent));

            assertEquals("Ошибка сериализации сообщения", exception.getMessage());
            assertNotNull(exception.getCause());
        }

        @Test
        void convertToJson_ShouldHandleNullEvent() throws Exception {
            // Arrange
            when(objectMapper.writeValueAsString(isNull())).thenReturn("null");

            // Act
            String result = messageConverter.convertToJson(null);

            // Assert
            assertEquals("null", result);
            verify(objectMapper).writeValueAsString(null);
        }
    }

    @Nested
    @DisplayName("Конвертация Message в MessageDto (convertToDto)")
    class ConvertToDtoTests {

        @Test
        void convertToDto_ShouldConvertMessageToDto_WhenValidMessage() {
            // Act
            MessageDto result = messageConverter.convertToDto(testMessage);

            // Assert
            assertNotNull(result);
            assertEquals(testMessage.getId(), result.getId());
            assertEquals(testMessage.getText(), result.getText());
            assertEquals(testMessage.getCreatedAt().toLocalDateTime(), result.getCreatedAt());
            assertEquals(testMessage.getSender().getId(), result.getSenderId());
            assertEquals(testMessage.getChat().getId(), result.getChatId());
        }

        @Test
        void convertToDto_ShouldHandleNullChat() {
            // Arrange
            testMessage.setChat(null);

            // Act
            MessageDto result = messageConverter.convertToDto(testMessage);

            // Assert
            assertNotNull(result);
            assertNull(result.getChatId());
            assertEquals(testMessage.getSender().getId(), result.getSenderId());
            assertEquals(testMessage.getText(), result.getText());
        }

        @Test
        void convertToDto_ShouldHandleMinimumMessageData() {
            // Arrange
            Message minimalMessage = new Message();
            minimalMessage.setId(1L);
            minimalMessage.setText("Minimal");
            minimalMessage.setCreatedAt(OffsetDateTime.now());
            minimalMessage.setSender(testSender);
            // chat is null

            // Act
            MessageDto result = messageConverter.convertToDto(minimalMessage);

            // Assert
            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("Minimal", result.getText());
            assertNull(result.getChatId());
            assertEquals(1L, result.getSenderId());
        }

        @Test
        void convertToDto_ShouldHandleNullSender() {
            // Arrange
            testMessage.setSender(null);

            // Act & Assert
            assertThrows(NullPointerException.class, () -> messageConverter.convertToDto(testMessage));
        }

        @Test
        void convertToDto_ShouldHandleNullCreatedAt() {
            // Arrange
            testMessage.setCreatedAt(null);

            // Act & Assert
            assertThrows(NullPointerException.class, () -> messageConverter.convertToDto(testMessage));
        }
    }

    @Nested
    @DisplayName("Создание события из JSON и senderId (createMessageEvent)")
    class CreateMessageEventTests {

        @Test
        void createMessageEvent_ShouldCreateEventWithSenderId_WhenValidJson() throws Exception {
            // Arrange
            String rawJson = "{\"type\":\"PRIVATE\",\"recipientId\":2,\"text\":\"Hello\"}";
            Long senderId = 123L;

            MessageEventDto baseEvent = new MessageEventDto();
            baseEvent.setType(MessageType.PRIVATE);
            baseEvent.setRecipientId(2L);
            baseEvent.setText("Hello");

            when(objectMapper.readValue(rawJson, MessageEventDto.class)).thenReturn(baseEvent);

            // Act
            MessageEventDto result = messageConverter.createMessageEvent(rawJson, senderId);

            // Assert
            assertNotNull(result);
            assertEquals(senderId, result.getSenderId());
            assertEquals(MessageType.PRIVATE, result.getType());
            assertEquals(2L, result.getRecipientId());
            assertEquals("Hello", result.getText());
            assertNotNull(result.getTimestamp());
            verify(objectMapper).readValue(rawJson, MessageEventDto.class);
        }

        @Test
        void createMessageEvent_ShouldThrowRuntimeException_WhenInvalidJson() throws Exception {
            // Arrange
            String invalidJson = "invalid";
            Long senderId = 123L;

            when(objectMapper.readValue(invalidJson, MessageEventDto.class))
                    .thenThrow(new JsonProcessingException("Invalid JSON") {});

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> messageConverter.createMessageEvent(invalidJson, senderId));

            assertEquals("Неверный формат сообщения", exception.getMessage());
        }

        @Test
        void createMessageEvent_ShouldCallValidate_WhenEventCreated() throws Exception {
            // Arrange
            String rawJson = "{\"type\":\"PRIVATE\",\"recipientId\":2,\"text\":\"Hello\"}";
            Long senderId = 123L;

            MessageEventDto mockEvent = mock(MessageEventDto.class);
            when(mockEvent.getType()).thenReturn(MessageType.PRIVATE);
            when(mockEvent.getRecipientId()).thenReturn(2L);
            when(mockEvent.getText()).thenReturn("Hello");

            when(objectMapper.readValue(rawJson, MessageEventDto.class)).thenReturn(mockEvent);
            doNothing().when(mockEvent).validate();

            // Act
            messageConverter.createMessageEvent(rawJson, senderId);

            // Assert
            verify(mockEvent).setSenderId(senderId);
            verify(mockEvent).setTimestamp(any(Instant.class));
            verify(mockEvent).validate();
        }

        @Test
        void createMessageEvent_ShouldOverrideSenderId_WhenJsonContainsSenderId() throws Exception {
            // Arrange
            String jsonWithSender = "{\"type\":\"PRIVATE\",\"senderId\":999,\"recipientId\":2,\"text\":\"Test message\"}";
            Long actualSenderId = 123L;

            MessageEventDto eventFromJson = new MessageEventDto();
            eventFromJson.setType(MessageType.PRIVATE);
            eventFromJson.setSenderId(999L); // будет перезаписано
            eventFromJson.setRecipientId(2L);
            eventFromJson.setText("Test message");

            when(objectMapper.readValue(jsonWithSender, MessageEventDto.class)).thenReturn(eventFromJson);

            // Act
            MessageEventDto result = messageConverter.createMessageEvent(jsonWithSender, actualSenderId);

            // Assert
            assertEquals(actualSenderId, result.getSenderId()); // Должен быть перезаписан
            assertEquals("Test message", result.getText()); // Текст должен быть установлен
        }
    }

    @Nested
    @DisplayName("Формирование JSON-ответов (createSuccessResponse / createErrorResponse)")
    class JsonResponseTests {

        @Test
        void createSuccessResponse_ShouldReturnSuccessJson_WhenSerializationSucceeds() throws Exception {
            // Arrange
            String expectedJson = "{\"status\":\"SENT\",\"timestamp\":\"2023-01-01T10:00:00\"}";
            when(objectMapper.writeValueAsString(any(Map.class))).thenReturn(expectedJson);

            // Act
            String result = messageConverter.createSuccessResponse();

            // Assert
            assertNotNull(result);
            assertEquals(expectedJson, result);
            // Упрощенная проверка без argThat для Map
            verify(objectMapper).writeValueAsString(any(Map.class));
        }

        @Test
        void createSuccessResponse_ShouldReturnFallbackJson_WhenSerializationFails() throws Exception {
            // Arrange
            when(objectMapper.writeValueAsString(any(Map.class)))
                    .thenThrow(new JsonProcessingException("Failed") {});

            // Act
            String result = messageConverter.createSuccessResponse();

            // Assert
            assertNotNull(result);
            assertTrue(result.contains("ERROR"));
            assertTrue(result.contains("Ошибка формирования ответа"));
        }

        @Test
        void createSuccessResponse_ShouldContainCorrectFields() throws Exception {
            // Arrange
            when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("success");

            // Act
            messageConverter.createSuccessResponse();

            // Assert
            verify(objectMapper).writeValueAsString(argThat((Map<String, Object> map) ->
                    map.containsKey("status") &&
                            map.containsKey("timestamp") &&
                            "SENT".equals(map.get("status"))
            ));
        }

        @Test
        void createErrorResponse_ShouldReturnErrorJson_WhenValidParameters() throws Exception {
            // Arrange
            String expectedJson = "{\"type\":\"ERROR\",\"code\":\"VALIDATION_ERROR\",\"message\":\"Invalid input\"}";
            when(objectMapper.writeValueAsString(any(Map.class))).thenReturn(expectedJson);

            // Act
            String result = messageConverter.createErrorResponse("VALIDATION_ERROR", "Invalid input");

            // Assert
            assertNotNull(result);
            assertEquals(expectedJson, result);
            verify(objectMapper).writeValueAsString(any(Map.class));
        }

        @Test
        void createErrorResponse_ShouldReturnFallbackJson_WhenSerializationFails() throws Exception {
            // Arrange
            when(objectMapper.writeValueAsString(any(Map.class)))
                    .thenThrow(new JsonProcessingException("Failed") {});

            // Act
            String result = messageConverter.createErrorResponse("VALIDATION_ERROR", "Invalid input");

            // Assert
            assertNotNull(result);
            assertTrue(result.contains("ERROR"));
            assertTrue(result.contains("SERVER_ERROR"));
            assertTrue(result.contains("Внутренняя ошибка сервера"));
        }

        @Test
        void createErrorResponse_ShouldHandleNullParameters() throws Exception {
            // Arrange
            String expectedJson = "{\"type\":\"ERROR\",\"code\":\"SERVER_ERROR\",\"message\":\"Внутренняя ошибка сервера\"}";
            when(objectMapper.writeValueAsString(any(Map.class))).thenReturn(expectedJson);

            // Act
            String result = messageConverter.createErrorResponse(null, null);

            // Assert
            assertNotNull(result);
            assertEquals(expectedJson, result);
        }

        @Test
        void createErrorResponse_ShouldContainCorrectFields() throws Exception {
            // Arrange
            when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("error");

            // Act
            messageConverter.createErrorResponse("TEST_CODE", "Test message");

            // Assert
            verify(objectMapper).writeValueAsString(argThat((Map<String, Object> map) ->
                    map.containsKey("type") &&
                            map.containsKey("code") &&
                            map.containsKey("message") &&
                            map.containsKey("timestamp") &&
                            "ERROR".equals(map.get("type")) &&
                            "TEST_CODE".equals(map.get("code")) &&
                            "Test message".equals(map.get("message"))
            ));
        }
    }
}