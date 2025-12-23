package com.example.kwizi.scheduler;

import com.example.kwizi.DTO.internal.MessageEventDto;
import com.example.kwizi.util.MessageConverter;
import com.example.kwizi.websocket.UniversalChatHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DlqRetrySchedulerTest {

    @Mock
    private MessageConverter messageConverter;

    @Mock
    private UniversalChatHandler chatHandler;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private DlqRetryScheduler scheduler;

    @Test
    void retryPrivateMessages_DLQFormat_UserOnline_ShouldSendMessage() throws Exception {
        // Arrange
        String dlqMessage = "{\"originalEvent\":{\"senderId\":1,\"text\":\"Hello\"},\"messageId\":123,\"recipientId\":2}";

        JsonNode jsonNode = mock(JsonNode.class);
        JsonNode originalEventNode = mock(JsonNode.class);
        JsonNode messageIdNode = mock(JsonNode.class);
        JsonNode recipientIdNode = mock(JsonNode.class);

        when(objectMapper.readTree(dlqMessage)).thenReturn(jsonNode);
        when(jsonNode.has("originalEvent")).thenReturn(true);
        when(jsonNode.has("messageId")).thenReturn(true);
        when(jsonNode.get("originalEvent")).thenReturn(originalEventNode);
        when(jsonNode.get("messageId")).thenReturn(messageIdNode);
        when(jsonNode.get("recipientId")).thenReturn(recipientIdNode);
        when(messageIdNode.asLong()).thenReturn(123L);
        when(recipientIdNode.asLong()).thenReturn(2L);

        MessageEventDto event = new MessageEventDto();
        event.setSenderId(1L);
        event.setRecipientId(2L);
        event.setText("Hello");
        when(objectMapper.convertValue(originalEventNode, MessageEventDto.class)).thenReturn(event);

        when(chatHandler.isUserOnline(2L)).thenReturn(true);

        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("test-json");

        // Act
        scheduler.retryPrivateMessages(dlqMessage);

        // Assert
        verify(chatHandler).sendToUser(eq(2L), anyString());
    }

    @Test
    void retryPrivateMessages_DirectFormat_UserOffline_ShouldNotSend() throws Exception {
        // Arrange
        String directMessage = "{\"senderId\":1,\"recipientId\":2,\"text\":\"Direct\"}";

        JsonNode jsonNode = mock(JsonNode.class);
        when(objectMapper.readTree(directMessage)).thenReturn(jsonNode);
        when(jsonNode.has("originalEvent")).thenReturn(false);

        MessageEventDto event = new MessageEventDto();
        event.setSenderId(1L);
        event.setRecipientId(2L);
        event.setText("Direct");
        when(messageConverter.convertToEvent(directMessage)).thenReturn(event);

        when(chatHandler.isUserOnline(2L)).thenReturn(false);

        // Act
        scheduler.retryPrivateMessages(directMessage);

        // Assert
        verify(chatHandler, never()).sendToUser(anyLong(), anyString());
    }

    @Test
    void retryGroupMessages_ValidGroup_UserOnline_ShouldSend() throws Exception {
        // Arrange
        String groupMessage = "{\"originalEvent\":{\"senderId\":1,\"text\":\"Group\"},\"messageId\":456,\"recipientId\":2,\"chatId\":100,\"messageType\":\"GROUP\"}";

        JsonNode jsonNode = mock(JsonNode.class);
        JsonNode originalEventNode = mock(JsonNode.class);
        JsonNode messageIdNode = mock(JsonNode.class);
        JsonNode recipientIdNode = mock(JsonNode.class);
        JsonNode chatIdNode = mock(JsonNode.class);
        JsonNode messageTypeNode = mock(JsonNode.class);

        when(objectMapper.readTree(groupMessage)).thenReturn(jsonNode);

        // Используем thenAnswer для гибкой обработки has() методов
        when(jsonNode.has(anyString())).thenAnswer(invocation -> {
            String field = invocation.getArgument(0);
            return field.equals("messageType") || field.equals("originalEvent") ||
                    field.equals("messageId") || field.equals("recipientId") ||
                    field.equals("chatId");
        });

        // Настраиваем get() методы
        when(jsonNode.get("messageType")).thenReturn(messageTypeNode);
        when(jsonNode.get("originalEvent")).thenReturn(originalEventNode);
        when(jsonNode.get("messageId")).thenReturn(messageIdNode);
        when(jsonNode.get("recipientId")).thenReturn(recipientIdNode);
        when(jsonNode.get("chatId")).thenReturn(chatIdNode);

        when(messageTypeNode.asText()).thenReturn("GROUP");
        when(messageIdNode.asLong()).thenReturn(456L);
        when(recipientIdNode.asLong()).thenReturn(2L);
        when(chatIdNode.asLong()).thenReturn(100L);

        MessageEventDto event = new MessageEventDto();
        event.setSenderId(1L);
        event.setRecipientId(2L);
        event.setText("Group");
        when(objectMapper.convertValue(originalEventNode, MessageEventDto.class)).thenReturn(event);

        when(chatHandler.isUserOnline(2L)).thenReturn(true);

        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("test-json");

        // Act
        scheduler.retryGroupMessages(groupMessage);

        // Assert
        verify(chatHandler).sendToUser(eq(2L), anyString());
    }

    @Test
    void retryGroupMessages_NotGroup_ShouldSkip() throws Exception {
        // Arrange
        String privateMessage = "{\"messageType\":\"PRIVATE\"}";

        JsonNode jsonNode = mock(JsonNode.class);
        JsonNode messageTypeNode = mock(JsonNode.class);

        when(objectMapper.readTree(privateMessage)).thenReturn(jsonNode);
        when(jsonNode.has("messageType")).thenReturn(true);
        when(jsonNode.get("messageType")).thenReturn(messageTypeNode);
        when(messageTypeNode.asText()).thenReturn("PRIVATE");

        // Act
        scheduler.retryGroupMessages(privateMessage);

        // Assert
        verify(chatHandler, never()).sendToUser(anyLong(), anyString());
    }

    @Test
    void scheduledMethods_ShouldNotThrowExceptions() {
        assertDoesNotThrow(() -> scheduler.scheduledDlqRetry());
        assertDoesNotThrow(() -> scheduler.cleanupInProgress());
        assertDoesNotThrow(() -> scheduler.healthCheck());
    }
}