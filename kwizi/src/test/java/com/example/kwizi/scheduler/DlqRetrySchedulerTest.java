package com.example.kwizi.scheduler;

import com.example.kwizi.DTO.internal.MessageEventDto;
import com.example.kwizi.util.MessageConverter;
import com.example.kwizi.websocket.UniversalChatHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@DisplayName("DlqRetryScheduler тесты")
@ExtendWith(MockitoExtension.class)
class DlqRetrySchedulerTest {

    @Mock
    private MessageConverter messageConverter;

    @Mock
    private UniversalChatHandler chatHandler;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private DlqRetryScheduler scheduler;

    @Nested
    @DisplayName("Повтор отправки личных сообщений")
    class RetryPrivateMessagesTests {

        @Test
        @DisplayName("✅ Отправляет сообщение, если пользователь онлайн и формат DLQ корректен")
        void retryPrivateMessages_DLQFormat_UserOnline_ShouldSendMessage() throws Exception {
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

            scheduler.retryPrivateMessages(dlqMessage);

            verify(chatHandler).sendToUser(eq(2L), anyString());
        }

        @Test
        @DisplayName("❌ Не отправляет сообщение, если пользователь оффлайн и сообщение в прямом формате")
        void retryPrivateMessages_DirectFormat_UserOffline_ShouldNotSend() throws Exception {
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

            scheduler.retryPrivateMessages(directMessage);

            verify(chatHandler, never()).sendToUser(anyLong(), anyString());
        }
    }

    @Nested
    @DisplayName("Повтор отправки групповых сообщений")
    class RetryGroupMessagesTests {

        @Test
        @DisplayName("✅ Отправляет сообщение, если пользователь онлайн и тип сообщения — GROUP")
        void retryGroupMessages_ValidGroup_UserOnline_ShouldSend() throws Exception {
            String groupMessage = "{\"originalEvent\":{\"senderId\":1,\"text\":\"Group\"},\"messageId\":456,\"recipientId\":2,\"chatId\":100,\"messageType\":\"GROUP\"}";

            JsonNode jsonNode = mock(JsonNode.class);
            JsonNode originalEventNode = mock(JsonNode.class);
            JsonNode messageIdNode = mock(JsonNode.class);
            JsonNode recipientIdNode = mock(JsonNode.class);
            JsonNode chatIdNode = mock(JsonNode.class);
            JsonNode messageTypeNode = mock(JsonNode.class);

            when(objectMapper.readTree(groupMessage)).thenReturn(jsonNode);
            when(jsonNode.has(anyString())).thenAnswer(invocation -> {
                String field = invocation.getArgument(0);
                return field.equals("messageType") || field.equals("originalEvent") ||
                        field.equals("messageId") || field.equals("recipientId") ||
                        field.equals("chatId");
            });
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

            scheduler.retryGroupMessages(groupMessage);

            verify(chatHandler).sendToUser(eq(2L), anyString());
        }

        @Test
        @DisplayName("❌ Пропускает обработку, если тип сообщения не GROUP")
        void retryGroupMessages_NotGroup_ShouldSkip() throws Exception {
            String privateMessage = "{\"messageType\":\"PRIVATE\"}";

            JsonNode jsonNode = mock(JsonNode.class);
            JsonNode messageTypeNode = mock(JsonNode.class);

            when(objectMapper.readTree(privateMessage)).thenReturn(jsonNode);
            when(jsonNode.has("messageType")).thenReturn(true);
            when(jsonNode.get("messageType")).thenReturn(messageTypeNode);
            when(messageTypeNode.asText()).thenReturn("PRIVATE");

            scheduler.retryGroupMessages(privateMessage);

            verify(chatHandler, never()).sendToUser(anyLong(), anyString());
        }
    }

    @Nested
    @DisplayName("Запланированные методы")
    class ScheduledMethodsTests {

        @Test
        @DisplayName("✅ Запланированные методы не выбрасывают исключений")
        void scheduledMethods_ShouldNotThrowExceptions() {
            assertThatNoException().isThrownBy(() -> scheduler.scheduledDlqRetry());
            assertThatNoException().isThrownBy(() -> scheduler.cleanupInProgress());
            assertThatNoException().isThrownBy(() -> scheduler.healthCheck());
        }
    }
}