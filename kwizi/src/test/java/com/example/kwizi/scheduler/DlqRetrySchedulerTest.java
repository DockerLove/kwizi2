package com.example.kwizi.scheduler;

import com.example.kwizi.DTO.internal.MessageEventDto;
import com.example.kwizi.model.Message;
import com.example.kwizi.repository.MessageRepository;
import com.example.kwizi.util.MessageConverter;
import com.example.kwizi.websocket.UniversalChatHandler;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("DlqRetryScheduler тесты (новая архитектура с assign)")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DlqRetrySchedulerTest {

    @Mock
    private UniversalChatHandler chatHandler;

    @Mock
    private ObjectMapper objectMapper;

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
        @DisplayName("Возвращает false, если сообщение не найдено в БД")
        void isMessageDeleted_MessageNotFound_ShouldReturnFalse() {
            Long messageId = 123L;
            when(messageRepository.findById(messageId)).thenReturn(Optional.empty());

            boolean result = ReflectionTestUtils.invokeMethod(scheduler, "isMessageDeleted", messageId);
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Обработка приватных сообщений из DLQ (processPrivateDlqMessage)")
    class ProcessPrivateDlqMessageTests {

        @Test
        @DisplayName("Пропускает доставку удаленного сообщения и коммитит оффсет")
        void processPrivateDlqMessage_MessageDeleted_ShouldSkipAndCommitOffset() {
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

            boolean result = ReflectionTestUtils.invokeMethod(
                    scheduler, "processPrivateDlqMessage",
                    event, messageId, recipientId, record, offsetsToCommit
            );

            assertThat(result).isFalse();
            assertThat(offsetsToCommit).hasSize(1);
            verify(chatHandler, never()).sendToUser(anyLong(), anyString());
        }

        @Test
        @DisplayName("Успешно доставляет сообщение онлайн-пользователю и коммитит оффсет")
        void processPrivateDlqMessage_UserOnline_ShouldDeliverAndCommitOffset() throws Exception {
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
            when(message.isDeleted()).thenReturn(false);
            when(chatHandler.isUserOnline(recipientId)).thenReturn(true);
            when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("mocked-payload");

            boolean result = ReflectionTestUtils.invokeMethod(
                    scheduler, "processPrivateDlqMessage",
                    event, messageId, recipientId, record, offsetsToCommit
            );

            assertThat(result).isTrue();
            assertThat(offsetsToCommit).hasSize(1);
            verify(chatHandler, times(1)).sendToUser(eq(recipientId), anyString());
        }

        @Test
        @DisplayName("Не коммитит оффсет, если пользователь оффлайн")
        void processPrivateDlqMessage_UserOffline_ShouldNotCommitOffset() {
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
            when(message.isDeleted()).thenReturn(false);
            when(chatHandler.isUserOnline(recipientId)).thenReturn(false);

            boolean result = ReflectionTestUtils.invokeMethod(
                    scheduler, "processPrivateDlqMessage",
                    event, messageId, recipientId, record, offsetsToCommit
            );

            assertThat(result).isFalse();
            assertThat(offsetsToCommit).isEmpty();
            verify(chatHandler, never()).sendToUser(anyLong(), anyString());
        }
    }

    @Nested
    @DisplayName("Обработка групповых сообщений из DLQ (processGroupDlqMessage)")
    class ProcessGroupDlqMessageTests {

        @Test
        @DisplayName("Успешно доставляет групповое сообщение онлайн-пользователю")
        void processGroupDlqMessage_UserOnline_ShouldDeliverAndCommit() throws Exception {
            Long chatId = 100L;

            MessageEventDto event = new MessageEventDto();
            event.setSenderId(1L);
            event.setText("Group Test");
            event.setChatId(chatId);

            Long messageId = 456L;
            Long recipientId = 2L;

            ConsumerRecord<String, String> record = mock(ConsumerRecord.class);
            when(record.topic()).thenReturn("group-messages-dlq");
            when(record.partition()).thenReturn(1);
            when(record.offset()).thenReturn(50L);

            Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = new HashMap<>();

            JsonNode chatIdNode = mock(JsonNode.class);
            when(chatIdNode.asLong()).thenReturn(chatId);

            JsonNode dlqNode = mock(JsonNode.class);
            when(dlqNode.has("chatId")).thenReturn(true);
            when(dlqNode.get("chatId")).thenReturn(chatIdNode);

            when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
            when(message.isDeleted()).thenReturn(false);
            when(chatHandler.isUserOnline(recipientId)).thenReturn(true);
            when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("mocked-payload");

            boolean result = ReflectionTestUtils.invokeMethod(
                    scheduler, "processGroupDlqMessage",
                    event, messageId, recipientId, dlqNode, record, offsetsToCommit
            );

            assertThat(result).isTrue();
            assertThat(offsetsToCommit).hasSize(1);
            verify(chatHandler, times(1)).sendToUser(eq(recipientId), anyString());
        }

        @Test
        @DisplayName("Возвращает false и не коммитит, если targetRecipientId равен null")
        void processGroupDlqMessage_NullRecipient_ShouldReturnFalse() {
            MessageEventDto event = new MessageEventDto();
            event.setSenderId(1L);
            event.setText("Group Test");
            event.setChatId(100L);

            ConsumerRecord<String, String> record = mock(ConsumerRecord.class);
            when(record.value()).thenReturn("{\"messageType\":\"GROUP\"}");

            Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = new HashMap<>();

            JsonNode chatIdNode = mock(JsonNode.class);
            when(chatIdNode.asLong()).thenReturn(100L);

            JsonNode dlqNode = mock(JsonNode.class);
            when(dlqNode.has("chatId")).thenReturn(true);
            when(dlqNode.get("chatId")).thenReturn(chatIdNode);

            boolean result = ReflectionTestUtils.invokeMethod(
                    scheduler, "processGroupDlqMessage",
                    event, null, null, dlqNode, record, offsetsToCommit
            );

            assertThat(result).isFalse();
            assertThat(offsetsToCommit).isEmpty();
        }
    }

    @Nested
    @DisplayName("Парсинг записи из топика (processDlqRecordFromTopic)")
    class ProcessDlqRecordFromTopicTests {

        @Test
        @DisplayName("Корректно парсит DLQ-формат и делегирует обработку")
        void processDlqRecordFromTopic_DlqFormat_ShouldDelegate() throws Exception {
            String jsonValue = "{\"messageId\":123, \"recipientId\":2, \"originalEvent\":{\"senderId\":1, \"text\":\"hi\"}}";

            ConsumerRecord<String, String> record = mock(ConsumerRecord.class);
            when(record.topic()).thenReturn("private-messages-dlq");
            when(record.partition()).thenReturn(0);
            when(record.offset()).thenReturn(10L);
            when(record.value()).thenReturn(jsonValue);

            Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = new HashMap<>();

            JsonNode recipientIdNode = mock(JsonNode.class);
            when(recipientIdNode.asLong()).thenReturn(2L);

            JsonNode messageIdNode = mock(JsonNode.class);
            when(messageIdNode.asLong()).thenReturn(123L);

            JsonNode originalEventNode = mock(JsonNode.class);

            JsonNode dlqNode = mock(JsonNode.class);
            when(dlqNode.has("originalEvent")).thenReturn(true);
            when(dlqNode.has("recipientId")).thenReturn(true);
            when(dlqNode.has("messageId")).thenReturn(true);

            when(dlqNode.get("originalEvent")).thenReturn(originalEventNode);
            when(dlqNode.get("recipientId")).thenReturn(recipientIdNode);
            when(dlqNode.get("messageId")).thenReturn(messageIdNode);
            MessageEventDto event = new MessageEventDto();
            event.setSenderId(1L);
            event.setRecipientId(2L);
            event.setText("hi");

            when(objectMapper.convertValue(any(JsonNode.class), eq(MessageEventDto.class))).thenReturn(event);
            when(objectMapper.readTree(jsonValue)).thenReturn(dlqNode);

            when(messageRepository.findById(123L)).thenReturn(Optional.of(message));
            when(message.isDeleted()).thenReturn(false);
            when(chatHandler.isUserOnline(2L)).thenReturn(false);

            boolean result = ReflectionTestUtils.invokeMethod(
                    scheduler, "processDlqRecordFromTopic",
                    record, offsetsToCommit, "private-messages-dlq"
            );

            assertThat(result).isFalse();
            verify(objectMapper, times(1)).readTree(jsonValue);
        }
    }

    @Nested
    @DisplayName("Безопасное извлечение messageId (extractMessageIdSafe)")
    class ExtractMessageIdSafeTests {

        @Test
        @DisplayName("Извлекает messageId из валидного JSON")
        void extractMessageIdSafe_ValidJson_ShouldReturnId() {
            com.fasterxml.jackson.databind.ObjectMapper realMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            ReflectionTestUtils.setField(scheduler, "objectMapper", realMapper);

            String json = "{\"messageId\": 999, \"other\": \"data\"}";

            Long result = ReflectionTestUtils.invokeMethod(scheduler, "extractMessageIdSafe", json);

            assertThat(result).isEqualTo(999L);
        }

        @Test
        @DisplayName("Возвращает hashCode при невалидном JSON")
        void extractMessageIdSafe_InvalidJson_ShouldReturnHashCode() throws Exception {
            String invalidJson = "not-a-json";
            when(objectMapper.readTree(invalidJson)).thenThrow(new RuntimeException("Parse error"));

            Long result = ReflectionTestUtils.invokeMethod(scheduler, "extractMessageIdSafe", invalidJson);
            assertThat(result).isEqualTo((long) Math.abs(invalidJson.hashCode()));
        }
    }

    @Nested
    @DisplayName("Запланированные методы (Scheduled)")
    class ScheduledMethodsTests {

        @Test
        @DisplayName("Cleanup удаляет все сообщения из in-progress")
        void cleanupInProgress_ShouldClearMessagesInProgress() {
            Set<Long> messagesInProgress = (Set<Long>) ReflectionTestUtils.getField(scheduler, "messagesInProgress");
            messagesInProgress.add(1L);
            messagesInProgress.add(2L);

            scheduler.cleanupInProgress();

            assertThat(messagesInProgress).isEmpty();
        }

        @Test
        @DisplayName("HealthCheck выполняется без исключений")
        void healthCheck_ShouldNotThrowExceptions() {
            assertThatNoException().isThrownBy(() -> scheduler.healthCheck());
        }

    }
}