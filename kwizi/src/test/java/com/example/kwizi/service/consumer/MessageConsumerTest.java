package com.example.kwizi.service.consumer;

import com.example.kwizi.DTO.internal.MessageDto;
import com.example.kwizi.DTO.internal.MessageEventDto;
import com.example.kwizi.enums.MessageType;
import com.example.kwizi.model.Message;
import com.example.kwizi.service.ChatMessageService;
import com.example.kwizi.util.MessageConverter;
import com.example.kwizi.websocket.UniversalChatHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageConsumerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private MessageConverter messageConverter;

    @Mock
    private ChatMessageService chatMessageService;

    @Mock
    private UniversalChatHandler chatHandler;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private MessageConsumer messageConsumer;

    @Captor
    private ArgumentCaptor<String> kafkaPayloadCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> messagePayloadCaptor;

    private MessageEventDto privateMessageEvent;
    private MessageEventDto groupMessageEvent;
    private Message testMessage;
    private MessageDto testMessageDto;

    @BeforeEach
    void setUp() {
        // Setup private message event
        privateMessageEvent = new MessageEventDto();
        privateMessageEvent.setType(MessageType.PRIVATE);
        privateMessageEvent.setSenderId(1L);
        privateMessageEvent.setRecipientId(2L);
        privateMessageEvent.setText("Hello private!");
        privateMessageEvent.setChatId(null);

        // Setup group message event
        groupMessageEvent = new MessageEventDto();
        groupMessageEvent.setType(MessageType.GROUP);
        groupMessageEvent.setSenderId(1L);
        groupMessageEvent.setChatId(100L);
        groupMessageEvent.setText("Hello group!");

        // Setup test message
        testMessage = new Message();
        testMessage.setId(999L);
        testMessage.setText("Test message");

        // Setup message DTO
        testMessageDto = new MessageDto();
        testMessageDto.setText("Test message");
    }

    // ✅ Test for consumeMessage with private message
    @Test
    void consumeMessage_ShouldProcessPrivateMessage_WhenPrivateEventReceived() throws Exception {
        // Arrange
        String kafkaMessage = "private-message-json";
        String topic = "private-messages";

        when(messageConverter.convertToEvent(kafkaMessage)).thenReturn(privateMessageEvent);
        when(chatMessageService.sendPrivateMessage(any(MessageDto.class), eq(1L), eq(2L)))
                .thenReturn(testMessage);
        when(chatHandler.isUserOnline(2L)).thenReturn(true);
        when(messageConverter.convertToDto(testMessage)).thenReturn(testMessageDto);

        // Act
        messageConsumer.consumeMessage(kafkaMessage, topic);

        // Assert
        verify(messageConverter).convertToEvent(kafkaMessage);
        verify(chatMessageService).sendPrivateMessage(any(MessageDto.class), eq(1L), eq(2L));
        verify(chatHandler).isUserOnline(2L);
        verify(chatHandler).sendToUser(eq(2L), messagePayloadCaptor.capture());

        Map<String, Object> sentMessage = messagePayloadCaptor.getValue();
        assertEquals("PRIVATE_MESSAGE", sentMessage.get("type"));
        assertEquals(testMessageDto, sentMessage.get("message"));
    }

    @Test
    void consumeMessage_ShouldSendPrivateMessageToDlq_WhenRecipientOffline() throws Exception {
        // Arrange
        String kafkaMessage = "private-message-json";
        String topic = "private-messages";

        when(messageConverter.convertToEvent(kafkaMessage)).thenReturn(privateMessageEvent);
        when(chatMessageService.sendPrivateMessage(any(MessageDto.class), eq(1L), eq(2L)))
                .thenReturn(testMessage);
        when(chatHandler.isUserOnline(2L)).thenReturn(false);
        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("dlq-payload");

        // Act
        messageConsumer.consumeMessage(kafkaMessage, topic);

        // Assert
        verify(kafkaTemplate).send(eq("private-messages-dlq"), kafkaPayloadCaptor.capture());
        verify(chatHandler, never()).sendToUser(eq(2L), any());

        String dlqPayload = kafkaPayloadCaptor.getValue();
        assertEquals("dlq-payload", dlqPayload);
    }

    // ✅ Test for consumeMessage with group message
    @Test
    void consumeMessage_ShouldProcessGroupMessage_WhenGroupEventReceived() throws Exception {
        // Arrange
        String kafkaMessage = "group-message-json";
        List<Long> chatMembers = List.of(1L, 2L, 3L);

        when(messageConverter.convertToEvent(kafkaMessage)).thenReturn(groupMessageEvent);
        when(chatMessageService.sendMessage(any(MessageDto.class), eq(1L))).thenReturn(testMessage);
        when(chatMessageService.getChatMembers(100L)).thenReturn(chatMembers);
        when(messageConverter.convertToDto(testMessage)).thenReturn(testMessageDto);
        when(chatHandler.isUserOnline(2L)).thenReturn(true);
        when(chatHandler.isUserOnline(3L)).thenReturn(true);

        // Act
        messageConsumer.consumeMessage(kafkaMessage, "group-messages");

        // Assert - проверяем взаимодействия с каждым пользователем отдельно
        verify(chatHandler).sendToUser(eq(2L), argThat(hasGroupMessageProperties()));  // ✅ добавить argThat()
        verify(chatHandler).sendToUser(eq(3L), argThat(hasGroupMessageProperties()));  // ✅ добавить argThat()
        verify(chatHandler, never()).sendToUser(eq(1L), argThat(hasGroupMessageProperties())); // ✅ добавить argThat()
    }

    // ✅ Вспомогательный метод для читаемости
    private ArgumentMatcher<Map<String, Object>> hasGroupMessageProperties() {
        return payload ->
                "GROUP_MESSAGE".equals(payload.get("type")) &&
                        Long.valueOf(100L).equals(payload.get("chatId")) &&
                        testMessageDto.equals(payload.get("message"));
    }
    @Test
    void consumeMessage_ShouldSendGroupMessagesToDlq_WhenMembersOffline() throws Exception {
        // Arrange
        String kafkaMessage = "group-message-json";
        List<Long> chatMembers = List.of(1L, 2L, 3L, 4L); // 4 пользователя

        when(messageConverter.convertToEvent(kafkaMessage)).thenReturn(groupMessageEvent);
        when(chatMessageService.sendMessage(any(MessageDto.class), eq(1L))).thenReturn(testMessage);
        when(chatMessageService.getChatMembers(100L)).thenReturn(chatMembers);
        when(messageConverter.convertToDto(testMessage)).thenReturn(testMessageDto);

        // 2 онлайн, 2 оффлайн
        when(chatHandler.isUserOnline(2L)).thenReturn(true);
        when(chatHandler.isUserOnline(3L)).thenReturn(true);
        when(chatHandler.isUserOnline(4L)).thenReturn(false);

        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("dlq-payload");

        // Act
        messageConsumer.consumeMessage(kafkaMessage, "group-messages");

        // Assert
        // ✅ Проверяем что онлайн-пользователи получили сообщение
        verify(chatHandler).sendToUser(eq(2L), argThat(hasGroupMessageProperties()));
        verify(chatHandler).sendToUser(eq(3L), argThat(hasGroupMessageProperties()));

        // ✅ Проверяем что оффлайн-пользователь НЕ получил сообщение
        verify(chatHandler, never()).sendToUser(eq(4L), any());

        // ✅ Проверяем что сообщение отправлено в DLQ для оффлайн-пользователя
        verify(kafkaTemplate).send(eq("group-messages-dlq"), anyString());

        // ✅ Проверяем общее количество отправок (только 2 онлайн-пользователя)
        verify(chatHandler, times(2)).sendToUser(anyLong(), any(Map.class));
    }

    // ✅ Test for delivery confirmation
    @Test
    void consumeMessage_ShouldSendDeliveryConfirmation_ForPrivateMessage() throws Exception {
        // Arrange
        String kafkaMessage = "private-message-json";
        String topic = "private-messages";

        when(messageConverter.convertToEvent(kafkaMessage)).thenReturn(privateMessageEvent);
        when(chatMessageService.sendPrivateMessage(any(MessageDto.class), eq(1L), eq(2L)))
                .thenReturn(testMessage);
        when(chatHandler.isUserOnline(2L)).thenReturn(true);
        when(messageConverter.convertToDto(testMessage)).thenReturn(testMessageDto);

        // Act
        messageConsumer.consumeMessage(kafkaMessage, topic);

        // Assert
        // Should send delivery confirmation to sender
        verify(chatHandler).sendToUser(eq(1L), messagePayloadCaptor.capture());

        Map<String, Object> confirmation = messagePayloadCaptor.getAllValues().stream()
                .filter(msg -> "DELIVERY_CONFIRMATION".equals(msg.get("type")))
                .findFirst()
                .orElse(null);

        assertNotNull(confirmation);
        assertEquals(999L, confirmation.get("messageId"));
        assertEquals("DELIVERED", confirmation.get("status"));
    }

    // ✅ Test for exception handling
    @Test
    void consumeMessage_ShouldThrowRuntimeException_WhenProcessingFails() throws Exception {
        // Arrange
        String kafkaMessage = "invalid-message-json";
        String topic = "private-messages";

        when(messageConverter.convertToEvent(kafkaMessage))
                .thenThrow(new RuntimeException("Conversion failed"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> messageConsumer.consumeMessage(kafkaMessage, topic));

        assertEquals("Ошибка обработки Kafka сообщения", exception.getMessage());
        verify(chatMessageService, never()).sendPrivateMessage(any(), anyLong(), anyLong());
    }

}