package com.example.kwizi.scheduler;

import com.example.kwizi.DTO.internal.MessageEventDto;
import com.example.kwizi.model.Message;
import com.example.kwizi.repository.MessageRepository;
import com.example.kwizi.util.MessageConverter;
import com.example.kwizi.websocket.UniversalChatHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@DisplayName("DlqRetryScheduler тесты")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DlqRetrySchedulerTest {

    @Mock
    private MessageConverter messageConverter;

    @Mock
    private UniversalChatHandler chatHandler;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private Message message;

    @InjectMocks
    private DlqRetryScheduler scheduler;

    @Nested
    @DisplayName("Проверка удаленных сообщений")
    class MessageDeletionCheckTests {

        @Test
        @DisplayName("Возвращает false, если messageId null")
        void isMessageDeleted_MessageIdNull_ShouldReturnFalse() {
            
            boolean result = ReflectionTestUtils.invokeMethod(scheduler, "isMessageDeleted", (Long) null);
            
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Возвращает true, если сообщение удалено")
        void isMessageDeleted_MessageDeleted_ShouldReturnTrue() {
            
            Long messageId = 123L;
            when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
            when(message.isDeleted()).thenReturn(true);
            
            boolean result = ReflectionTestUtils.invokeMethod(scheduler, "isMessageDeleted", messageId);
            
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Возвращает true, если сообщение не найдено в БД")
        void isMessageDeleted_MessageNotFound_ShouldReturnTrue() {
            
            Long messageId = 123L;
            when(messageRepository.findById(messageId)).thenReturn(Optional.empty());

            boolean result = ReflectionTestUtils.invokeMethod(scheduler, "isMessageDeleted", messageId);
            
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Возвращает false, если сообщение не удалено")
        void isMessageDeleted_MessageNotDeleted_ShouldReturnFalse() {
            
            Long messageId = 123L;
            when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
            when(message.isDeleted()).thenReturn(false);

            boolean result = ReflectionTestUtils.invokeMethod(scheduler, "isMessageDeleted", messageId);
            
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Возвращает false при ошибке БД")
        void isMessageDeleted_DatabaseError_ShouldReturnFalse() {
            
            Long messageId = 123L;
            when(messageRepository.findById(messageId)).thenThrow(new RuntimeException("DB error"));

            boolean result = ReflectionTestUtils.invokeMethod(scheduler, "isMessageDeleted", messageId);
            
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Повтор отправки личных сообщений")
    class RetryPrivateMessagesTests {

        @Test
        @DisplayName("Отправляет сообщение, если пользователь онлайн и сообщение не удалено")
        void retryPrivateMessages_DLQFormat_UserOnline_MessageNotDeleted_ShouldSendMessage() throws Exception {
            
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
            when(messageRepository.findById(123L)).thenReturn(Optional.of(message));
            when(message.isDeleted()).thenReturn(false);

            when(chatHandler.isUserOnline(2L)).thenReturn(true);
            when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("test-json");
            
            scheduler.retryPrivateMessages(dlqMessage);

            verify(chatHandler).sendToUser(eq(2L), anyString());
        }

        @Test
        @DisplayName("Не отправляет сообщение, если оно удалено")
        void retryPrivateMessages_MessageDeleted_ShouldSkip() throws Exception {
            
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
            when(messageRepository.findById(123L)).thenReturn(Optional.of(message));
            when(message.isDeleted()).thenReturn(true);

            scheduler.retryPrivateMessages(dlqMessage);
            
            verify(chatHandler, never()).sendToUser(anyLong(), anyString());
        }

        @Test
        @DisplayName("Не отправляет сообщение, если пользователь оффлайн и сообщение в прямом формате")
        void retryPrivateMessages_DirectFormat_UserOffline_ShouldNotSend() throws Exception {
            
            String directMessage = "{\"senderId\":1,\"recipientId\":2,\"text\":\"Direct\",\"timestamp\":\"2024-01-01T10:00:00Z\"}";

            JsonNode jsonNode = mock(JsonNode.class);
            when(objectMapper.readTree(directMessage)).thenReturn(jsonNode);
            when(jsonNode.has("originalEvent")).thenReturn(false);

            MessageEventDto event = new MessageEventDto();
            event.setSenderId(1L);
            event.setRecipientId(2L);
            event.setText("Direct");
            event.setTimestamp(Instant.parse("2024-01-01T10:00:00Z"));
            when(messageConverter.convertToEvent(directMessage)).thenReturn(event);
            when(chatHandler.isUserOnline(2L)).thenReturn(false);

            scheduler.retryPrivateMessages(directMessage);
            
            verify(chatHandler, never()).sendToUser(anyLong(), anyString());
        }

        @Test
        @DisplayName("Находит и проверяет удаление для сообщений без messageId в DLQ")
        void retryPrivateMessages_OldFormat_FindsAndChecksMessage() throws Exception {
            
            String oldFormatMessage = "{\"senderId\":1,\"recipientId\":2,\"text\":\"Old\",\"timestamp\":\"2024-01-01T10:00:00Z\"}";

            JsonNode jsonNode = mock(JsonNode.class);
            when(objectMapper.readTree(oldFormatMessage)).thenReturn(jsonNode);
            when(jsonNode.has("originalEvent")).thenReturn(false);

            MessageEventDto event = new MessageEventDto();
            event.setSenderId(1L);
            event.setRecipientId(2L);
            event.setText("Old");
            event.setTimestamp(Instant.parse("2024-01-01T10:00:00Z"));
            when(messageConverter.convertToEvent(oldFormatMessage)).thenReturn(event);

            Long foundMessageId = 456L;
            when(messageRepository.findMessageIdBySenderAndTextAndTime(
                    eq(1L), eq("Old"), any(Instant.class), any(Instant.class)
            )).thenReturn(Optional.of(foundMessageId));

            when(messageRepository.findById(foundMessageId)).thenReturn(Optional.of(message));
            when(message.isDeleted()).thenReturn(true);
            
            scheduler.retryPrivateMessages(oldFormatMessage);

            verify(chatHandler, never()).sendToUser(anyLong(), anyString());
        }
    }

    @Nested
    @DisplayName("Повтор отправки групповых сообщений")
    class RetryGroupMessagesTests {

        @Test
        @DisplayName("Отправляет сообщение, если пользователь онлайн, тип GROUP и сообщение не удалено")
        void retryGroupMessages_ValidGroup_UserOnline_MessageNotDeleted_ShouldSend() throws Exception {
            
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
            when(messageRepository.findById(456L)).thenReturn(Optional.of(message));
            when(message.isDeleted()).thenReturn(false);
            when(chatHandler.isUserOnline(2L)).thenReturn(true);
            when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("test-json");

            
            scheduler.retryGroupMessages(groupMessage);

            
            verify(chatHandler).sendToUser(eq(2L), anyString());
        }

        @Test
        @DisplayName("Пропускает отправку, если групповое сообщение удалено")
        void retryGroupMessages_MessageDeleted_ShouldSkip() throws Exception {
            
            String groupMessage = "{\"originalEvent\":{\"senderId\":1,\"text\":\"Group\"},\"messageId\":456,\"recipientId\":2,\"chatId\":100,\"messageType\":\"GROUP\"}";

            JsonNode jsonNode = mock(JsonNode.class);
            JsonNode messageTypeNode = mock(JsonNode.class);

            when(objectMapper.readTree(groupMessage)).thenReturn(jsonNode);
            when(jsonNode.has("messageType")).thenReturn(true);
            when(jsonNode.get("messageType")).thenReturn(messageTypeNode);
            when(messageTypeNode.asText()).thenReturn("GROUP");
            when(jsonNode.has("messageId")).thenReturn(true);
            when(jsonNode.get("messageId")).thenReturn(messageTypeNode);
            when(messageTypeNode.asLong()).thenReturn(456L);

            when(messageRepository.findById(456L)).thenReturn(Optional.of(message));
            when(message.isDeleted()).thenReturn(true);
            
            scheduler.retryGroupMessages(groupMessage);

            verify(chatHandler, never()).sendToUser(anyLong(), anyString());
            verify(objectMapper, never()).convertValue(any(JsonNode.class), eq(MessageEventDto.class));
        }

        @Test
        @DisplayName("Пропускает обработку, если тип сообщения не GROUP")
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

        @Test
        @DisplayName("Не отправляет сообщение, если пользователь оффлайн")
        void retryGroupMessages_UserOffline_ShouldNotSend() throws Exception {
            
            String groupMessage = "{\"originalEvent\":{\"senderId\":1,\"text\":\"Group\"},\"messageId\":456,\"recipientId\":2,\"chatId\":100,\"messageType\":\"GROUP\"}";

            JsonNode jsonNode = mock(JsonNode.class);
            JsonNode originalEventNode = mock(JsonNode.class);
            JsonNode messageIdNode = mock(JsonNode.class);
            JsonNode recipientIdNode = mock(JsonNode.class);
            JsonNode chatIdNode = mock(JsonNode.class);
            JsonNode messageTypeNode = mock(JsonNode.class);

            when(objectMapper.readTree(groupMessage)).thenReturn(jsonNode);
            when(jsonNode.has("messageType")).thenReturn(true);
            when(jsonNode.get("messageType")).thenReturn(messageTypeNode);
            when(messageTypeNode.asText()).thenReturn("GROUP");
            when(jsonNode.has("originalEvent")).thenReturn(true);
            when(jsonNode.has("messageId")).thenReturn(true);
            when(jsonNode.get("originalEvent")).thenReturn(originalEventNode);
            when(jsonNode.get("messageId")).thenReturn(messageIdNode);
            when(jsonNode.get("recipientId")).thenReturn(recipientIdNode);
            when(jsonNode.get("chatId")).thenReturn(chatIdNode);
            when(messageIdNode.asLong()).thenReturn(456L);
            when(recipientIdNode.asLong()).thenReturn(2L);
            when(chatIdNode.asLong()).thenReturn(100L);

            MessageEventDto event = new MessageEventDto();
            event.setSenderId(1L);
            event.setRecipientId(2L);
            event.setText("Group");
            when(objectMapper.convertValue(originalEventNode, MessageEventDto.class)).thenReturn(event);
            when(messageRepository.findById(456L)).thenReturn(Optional.of(message));
            when(message.isDeleted()).thenReturn(false);
            when(chatHandler.isUserOnline(2L)).thenReturn(false);

            
            scheduler.retryGroupMessages(groupMessage);

            
            verify(chatHandler, never()).sendToUser(anyLong(), anyString());
            verify(chatHandler, times(1)).isUserOnline(2L);
            verify(messageRepository, times(1)).findById(456L);
        }
    }

    @Nested
    @DisplayName("Поиск сообщения по event")
    class FindMessageIdByEventTests {

        @Test
        @DisplayName("Находит messageId по данным event")
        void findMessageIdByEvent_ShouldFindMessageId() throws Exception {
            
            MessageEventDto event = new MessageEventDto();
            event.setSenderId(1L);
            event.setText("Test message");
            event.setTimestamp(Instant.parse("2024-01-01T10:00:00Z"));

            Long expectedMessageId = 123L;
            when(messageRepository.findMessageIdBySenderAndTextAndTime(
                    eq(1L), eq("Test message"), any(Instant.class), any(Instant.class)
            )).thenReturn(Optional.of(expectedMessageId));
            
            Long result = ReflectionTestUtils.invokeMethod(scheduler, "findMessageIdByEvent", event);
            
            assertThat(result).isEqualTo(expectedMessageId);
        }

        @Test
        @DisplayName("Возвращает null, если сообщение не найдено")
        void findMessageIdByEvent_MessageNotFound_ShouldReturnNull() throws Exception {
            
            MessageEventDto event = new MessageEventDto();
            event.setSenderId(1L);
            event.setText("Test message");
            event.setTimestamp(Instant.parse("2024-01-01T10:00:00Z"));

            when(messageRepository.findMessageIdBySenderAndTextAndTime(
                    eq(1L), eq("Test message"), any(Instant.class), any(Instant.class)
            )).thenReturn(Optional.empty());
            
            Long result = ReflectionTestUtils.invokeMethod(scheduler, "findMessageIdByEvent", event);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Возвращает null при ошибке поиска")
        void findMessageIdByEvent_Exception_ShouldReturnNull() throws Exception {
            
            MessageEventDto event = new MessageEventDto();
            event.setSenderId(1L);
            event.setText("Test message");
            event.setTimestamp(Instant.parse("2024-01-01T10:00:00Z"));

            when(messageRepository.findMessageIdBySenderAndTextAndTime(
                    any(), any(), any(), any()
            )).thenThrow(new RuntimeException("Search error"));

            Long result = ReflectionTestUtils.invokeMethod(scheduler, "findMessageIdByEvent", event);
            
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Запланированные методы")
    class ScheduledMethodsTests {

        @Test
        @DisplayName("Запланированные методы не выбрасывают исключений")
        void scheduledMethods_ShouldNotThrowExceptions() {
            assertThatNoException().isThrownBy(() -> scheduler.scheduledDlqRetry());
            assertThatNoException().isThrownBy(() -> scheduler.cleanupInProgress());
            assertThatNoException().isThrownBy(() -> scheduler.healthCheck());
        }

        @Test
        @DisplayName("Cleanup удаляет все сообщения из in-progress")
        void cleanupInProgress_ShouldClearMessagesInProgress() {
            
            Set<Long> messagesInProgress = (Set<Long>) ReflectionTestUtils.getField(scheduler, "messagesInProgress");
            messagesInProgress.add(1L);
            messagesInProgress.add(2L);
            messagesInProgress.add(3L);

            scheduler.cleanupInProgress();
            
            assertThat(messagesInProgress).isEmpty();
        }
    }

    @Nested
    @DisplayName("Обработка приватных сообщений из DLQ")
    class ProcessPrivateDlqMessageTests {

        @Test
        @DisplayName("Пропускает доставку удаленного сообщения и коммитит оффсет")
        void processPrivateDlqMessage_MessageDeleted_ShouldSkipAndCommitOffset() throws Exception {
            
            MessageEventDto event = new MessageEventDto();
            event.setSenderId(1L);
            event.setRecipientId(2L);
            event.setText("Test");

            Long messageId = 123L;
            Long recipientId = 2L;

            ConsumerRecord<String, String> record = mock(ConsumerRecord.class);
            when(record.topic()).thenReturn("private-messages-dlq");
            when(record.partition()).thenReturn(0);
            when(record.offset()).thenReturn(100L);

            Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = new HashMap<>();

            when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
            when(message.isDeleted()).thenReturn(true);
            doNothing().when(chatHandler).sendToUser(anyLong(), anyString());

            boolean result = ReflectionTestUtils.invokeMethod(
                    scheduler, "processPrivateDlqMessage",
                    event, messageId, recipientId, record, offsetsToCommit
            );

            assertThat(result).isFalse();
            assertThat(offsetsToCommit).hasSize(1);
            verify(chatHandler, never()).sendToUser(anyLong(), anyString());
        }
    }

    @Nested
    @DisplayName("Обработка ошибок")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Обрабатывает ошибку JSON парсинга")
        void retryPrivateMessages_InvalidJson_ShouldHandleGracefully() throws Exception {
            
            String invalidJson = "invalid-json";
            when(objectMapper.readTree(invalidJson)).thenThrow(new JsonProcessingException("Invalid JSON") {});

            MessageEventDto event = new MessageEventDto();
            event.setSenderId(1L);
            event.setRecipientId(2L);
            event.setText("Test");
            when(messageConverter.convertToEvent(invalidJson)).thenReturn(event);

            assertThatNoException().isThrownBy(() -> scheduler.retryPrivateMessages(invalidJson));
        }
    }
}