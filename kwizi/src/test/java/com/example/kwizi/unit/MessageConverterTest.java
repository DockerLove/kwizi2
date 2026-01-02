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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    @DisplayName("Конвертация JSON в MessageEventDto")
    class ConvertToEventTests {

        @Test
        @DisplayName("Должен корректно конвертировать валидный JSON")
        void shouldReturnEvent_WhenValidJson() throws Exception {
            String validJson = "{\"type\":\"PRIVATE\",\"senderId\":1,\"recipientId\":2,\"text\":\"Hello\"}";
            when(objectMapper.readValue(validJson, MessageEventDto.class)).thenReturn(testEvent);

            MessageEventDto result = messageConverter.convertToEvent(validJson);

            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result).isEqualTo(testEvent)
            );
            verify(objectMapper).readValue(validJson, MessageEventDto.class);
        }

        @Test
        @DisplayName("Должен бросать RuntimeException при невалидном JSON")
        void shouldThrowRuntimeException_WhenInvalidJson() throws Exception {
            String invalidJson = "invalid json";
            when(objectMapper.readValue(invalidJson, MessageEventDto.class))
                    .thenThrow(new JsonProcessingException("Invalid JSON") {});

            assertThatThrownBy(() -> messageConverter.convertToEvent(invalidJson))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Неверный формат сообщения")
                    .hasCauseInstanceOf(JsonProcessingException.class);
        }

        @Test
        @DisplayName("Должен обрабатывать пустой JSON объект")
        void shouldHandleEmptyJson() throws Exception {
            String emptyJson = "{}";
            MessageEventDto emptyEvent = new MessageEventDto();
            when(objectMapper.readValue(emptyJson, MessageEventDto.class)).thenReturn(emptyEvent);

            MessageEventDto result = messageConverter.convertToEvent(emptyJson);

            assertThat(result).isNotNull();
            verify(objectMapper).readValue(emptyJson, MessageEventDto.class);
        }
    }

    @Nested
    @DisplayName("Сериализация MessageEventDto в JSON")
    class ConvertToJsonTests {

        @Test
        @DisplayName("Должен возвращать JSON при валидном событии")
        void shouldReturnJson_WhenValidEvent() throws Exception {
            String expectedJson = "{\"type\":\"PRIVATE\",\"text\":\"Hello\"}";
            when(objectMapper.writeValueAsString(testEvent)).thenReturn(expectedJson);

            String result = messageConverter.convertToJson(testEvent);

            assertThat(result).isEqualTo(expectedJson);
            verify(objectMapper).writeValueAsString(testEvent);
        }

        @Test
        @DisplayName("Должен бросать RuntimeException при ошибке сериализации")
        void shouldThrowRuntimeException_WhenSerializationFails() throws Exception {
            when(objectMapper.writeValueAsString(testEvent))
                    .thenThrow(new JsonProcessingException("Serialization failed") {});

            assertThatThrownBy(() -> messageConverter.convertToJson(testEvent))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Ошибка сериализации сообщения")
                    .hasCauseInstanceOf(JsonProcessingException.class);
        }

        @Test
        @DisplayName("Должен обрабатывать null событие")
        void shouldHandleNullEvent() throws Exception {
            when(objectMapper.writeValueAsString(isNull())).thenReturn("null");

            String result = messageConverter.convertToJson(null);

            assertThat(result).isEqualTo("null");
            verify(objectMapper).writeValueAsString(null);
        }
    }

    @Nested
    @DisplayName("Конвертация Message в MessageDto")
    class ConvertToDtoTests {

        @Test
        @DisplayName("Должен корректно конвертировать Message в DTO")
        void shouldConvertMessageToDto_WhenValidMessage() {
            MessageDto result = messageConverter.convertToDto(testMessage);

            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getId()).isEqualTo(testMessage.getId()),
                    () -> assertThat(result.getText()).isEqualTo(testMessage.getText()),
                    () -> assertThat(result.getCreatedAt()).isEqualTo(testMessage.getCreatedAt().toLocalDateTime()),
                    () -> assertThat(result.getSenderId()).isEqualTo(testMessage.getSender().getId()),
                    () -> assertThat(result.getChatId()).isEqualTo(testMessage.getChat().getId())
            );
        }

        @Test
        @DisplayName("Должен обрабатывать null чат")
        void shouldHandleNullChat() {
            testMessage.setChat(null);

            MessageDto result = messageConverter.convertToDto(testMessage);

            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getChatId()).isNull(),
                    () -> assertThat(result.getSenderId()).isEqualTo(testMessage.getSender().getId()),
                    () -> assertThat(result.getText()).isEqualTo(testMessage.getText())
            );
        }

        @Test
        @DisplayName("Должен обрабатывать минимальные данные сообщения")
        void shouldHandleMinimumMessageData() {
            Message minimalMessage = new Message();
            minimalMessage.setId(1L);
            minimalMessage.setText("Minimal");
            minimalMessage.setCreatedAt(OffsetDateTime.now());
            minimalMessage.setSender(testSender);

            MessageDto result = messageConverter.convertToDto(minimalMessage);

            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getId()).isEqualTo(1L),
                    () -> assertThat(result.getText()).isEqualTo("Minimal"),
                    () -> assertThat(result.getChatId()).isNull(),
                    () -> assertThat(result.getSenderId()).isEqualTo(1L)
            );
        }

        @Test
        @DisplayName("Должен бросать NullPointerException при null отправителе")
        void shouldThrowNullPointerException_WhenNullSender() {
            testMessage.setSender(null);

            assertThatThrownBy(() -> messageConverter.convertToDto(testMessage))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Должен бросать NullPointerException при null дате создания")
        void shouldThrowNullPointerException_WhenNullCreatedAt() {
            testMessage.setCreatedAt(null);

            assertThatThrownBy(() -> messageConverter.convertToDto(testMessage))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Создание события из JSON и senderId")
    class CreateMessageEventTests {

        @Test
        @DisplayName("Должен создавать событие с senderId при валидном JSON")
        void shouldCreateEventWithSenderId_WhenValidJson() throws Exception {
            String rawJson = "{\"type\":\"PRIVATE\",\"recipientId\":2,\"text\":\"Hello\"}";
            Long senderId = 123L;

            MessageEventDto baseEvent = new MessageEventDto();
            baseEvent.setType(MessageType.PRIVATE);
            baseEvent.setRecipientId(2L);
            baseEvent.setText("Hello");

            when(objectMapper.readValue(rawJson, MessageEventDto.class)).thenReturn(baseEvent);

            MessageEventDto result = messageConverter.createMessageEvent(rawJson, senderId);

            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getSenderId()).isEqualTo(senderId),
                    () -> assertThat(result.getType()).isEqualTo(MessageType.PRIVATE),
                    () -> assertThat(result.getRecipientId()).isEqualTo(2L),
                    () -> assertThat(result.getText()).isEqualTo("Hello"),
                    () -> assertThat(result.getTimestamp()).isNotNull()
            );
            verify(objectMapper).readValue(rawJson, MessageEventDto.class);
        }

        @Test
        @DisplayName("Должен бросать RuntimeException при невалидном JSON")
        void shouldThrowRuntimeException_WhenInvalidJson() throws Exception {
            String invalidJson = "invalid";
            Long senderId = 123L;

            when(objectMapper.readValue(invalidJson, MessageEventDto.class))
                    .thenThrow(new JsonProcessingException("Invalid JSON") {});

            assertThatThrownBy(() -> messageConverter.createMessageEvent(invalidJson, senderId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Неверный формат сообщения");
        }

        @Test
        @DisplayName("Должен вызывать validate при создании события")
        void shouldCallValidate_WhenEventCreated() throws Exception {
            String rawJson = "{\"type\":\"PRIVATE\",\"recipientId\":2,\"text\":\"Hello\"}";
            Long senderId = 123L;

            MessageEventDto mockEvent = mock(MessageEventDto.class);
            when(mockEvent.getType()).thenReturn(MessageType.PRIVATE);
            when(mockEvent.getRecipientId()).thenReturn(2L);
            when(mockEvent.getText()).thenReturn("Hello");

            when(objectMapper.readValue(rawJson, MessageEventDto.class)).thenReturn(mockEvent);
            doNothing().when(mockEvent).validate();

            messageConverter.createMessageEvent(rawJson, senderId);

            verify(mockEvent).setSenderId(senderId);
            verify(mockEvent).setTimestamp(any(Instant.class));
            verify(mockEvent).validate();
        }

        @Test
        @DisplayName("Должен переопределять senderId из JSON")
        void shouldOverrideSenderId_WhenJsonContainsSenderId() throws Exception {
            String jsonWithSender = "{\"type\":\"PRIVATE\",\"senderId\":999,\"recipientId\":2,\"text\":\"Test message\"}";
            Long actualSenderId = 123L;

            MessageEventDto eventFromJson = new MessageEventDto();
            eventFromJson.setType(MessageType.PRIVATE);
            eventFromJson.setSenderId(999L);
            eventFromJson.setRecipientId(2L);
            eventFromJson.setText("Test message");

            when(objectMapper.readValue(jsonWithSender, MessageEventDto.class)).thenReturn(eventFromJson);

            MessageEventDto result = messageConverter.createMessageEvent(jsonWithSender, actualSenderId);

            assertAll(
                    () -> assertThat(result.getSenderId()).isEqualTo(actualSenderId),
                    () -> assertThat(result.getText()).isEqualTo("Test message")
            );
        }
    }

    @Nested
    @DisplayName("Формирование JSON-ответов")
    class JsonResponseTests {

        @Test
        @DisplayName("Должен возвращать успешный JSON при успешной сериализации")
        void shouldReturnSuccessJson_WhenSerializationSucceeds() throws Exception {
            String expectedJson = "{\"status\":\"SENT\",\"timestamp\":\"2023-01-01T10:00:00\"}";
            when(objectMapper.writeValueAsString(any(Map.class))).thenReturn(expectedJson);

            String result = messageConverter.createSuccessResponse();

            assertThat(result).isEqualTo(expectedJson);
            verify(objectMapper).writeValueAsString(any(Map.class));
        }

        @Test
        @DisplayName("Должен возвращать fallback JSON при ошибке сериализации")
        void shouldReturnFallbackJson_WhenSerializationFails() throws Exception {
            when(objectMapper.writeValueAsString(any(Map.class)))
                    .thenThrow(new JsonProcessingException("Failed") {});

            String result = messageConverter.createSuccessResponse();

            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result).contains("ERROR"),
                    () -> assertThat(result).contains("Ошибка формирования ответа")
            );
        }

        @Test
        @DisplayName("Должен содержать корректные поля успешного ответа")
        void shouldContainCorrectFields_ForSuccessResponse() throws Exception {
            when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("success");

            messageConverter.createSuccessResponse();

            verify(objectMapper).writeValueAsString(argThat((Map<String, Object> map) ->
                    map.containsKey("status") &&
                            map.containsKey("timestamp") &&
                            "SENT".equals(map.get("status"))
            ));
        }

        @Test
        @DisplayName("Должен возвращать JSON ошибки при валидных параметрах")
        void shouldReturnErrorJson_WhenValidParameters() throws Exception {
            String expectedJson = "{\"type\":\"ERROR\",\"code\":\"VALIDATION_ERROR\",\"message\":\"Invalid input\"}";
            when(objectMapper.writeValueAsString(any(Map.class))).thenReturn(expectedJson);

            String result = messageConverter.createErrorResponse("VALIDATION_ERROR", "Invalid input");

            assertThat(result).isEqualTo(expectedJson);
            verify(objectMapper).writeValueAsString(any(Map.class));
        }

        @Test
        @DisplayName("Должен возвращать fallback JSON при ошибке сериализации ошибки")
        void shouldReturnFallbackJson_WhenErrorSerializationFails() throws Exception {
            when(objectMapper.writeValueAsString(any(Map.class)))
                    .thenThrow(new JsonProcessingException("Failed") {});

            String result = messageConverter.createErrorResponse("VALIDATION_ERROR", "Invalid input");

            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result).contains("ERROR"),
                    () -> assertThat(result).contains("SERVER_ERROR"),
                    () -> assertThat(result).contains("Внутренняя ошибка сервера")
            );
        }

        @Test
        @DisplayName("Должен обрабатывать null параметры для ошибки")
        void shouldHandleNullParameters_ForErrorResponse() throws Exception {
            String expectedJson = "{\"type\":\"ERROR\",\"code\":\"SERVER_ERROR\",\"message\":\"Внутренняя ошибка сервера\"}";
            when(objectMapper.writeValueAsString(any(Map.class))).thenReturn(expectedJson);

            String result = messageConverter.createErrorResponse(null, null);

            assertThat(result).isEqualTo(expectedJson);
        }

        @Test
        @DisplayName("Должен содержать корректные поля ответа ошибки")
        void shouldContainCorrectFields_ForErrorResponse() throws Exception {
            when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("error");

            messageConverter.createErrorResponse("TEST_CODE", "Test message");

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