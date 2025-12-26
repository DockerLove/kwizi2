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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageConsumer тесты")
class MessageConsumerTest {

    @Mock private ObjectMapper objectMapper;
    @Mock private MessageConverter messageConverter;
    @Mock private ChatMessageService chatMessageService;
    @Mock private UniversalChatHandler chatHandler;
    @Mock private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private MessageConsumer messageConsumer;

    @Captor private ArgumentCaptor<String> kafkaPayloadCaptor;
    @Captor private ArgumentCaptor<Map<String, Object>> messagePayloadCaptor;

    private MessageEventDto privateMessageEvent;
    private MessageEventDto groupMessageEvent;
    private Message testMessage;
    private MessageDto testMessageDto;

    @BeforeEach
    void setUp() {
        privateMessageEvent = new MessageEventDto();
        privateMessageEvent.setType(MessageType.PRIVATE);
        privateMessageEvent.setSenderId(1L);
        privateMessageEvent.setRecipientId(2L);
        privateMessageEvent.setText("Hello private!");
        privateMessageEvent.setChatId(null);

        groupMessageEvent = new MessageEventDto();
        groupMessageEvent.setType(MessageType.GROUP);
        groupMessageEvent.setSenderId(1L);
        groupMessageEvent.setChatId(100L);
        groupMessageEvent.setText("Hello group!");

        testMessage = new Message();
        testMessage.setId(999L);
        testMessage.setText("Test message");

        testMessageDto = new MessageDto();
        testMessageDto.setText("Test message");
    }

    @Nested
    @DisplayName("Обработка приватного сообщения")
    class PrivateMessage {

        @Test
        @DisplayName("✅ Успешная обработка: получатель онлайн")
        void processesWhenRecipientOnline(){
            String kafkaMessage = "private-message-json";
            String topic = "private-messages";

            when(messageConverter.convertToEvent(kafkaMessage)).thenReturn(privateMessageEvent);
            when(chatMessageService.sendPrivateMessage(any(MessageDto.class), eq(1L), eq(2L)))
                    .thenReturn(testMessage);
            when(chatHandler.isUserOnline(2L)).thenReturn(true);
            when(messageConverter.convertToDto(testMessage)).thenReturn(testMessageDto);

            messageConsumer.consumeMessage(kafkaMessage, topic);

            verify(messageConverter).convertToEvent(kafkaMessage);
            verify(chatMessageService).sendPrivateMessage(any(MessageDto.class), eq(1L), eq(2L));
            verify(chatHandler).isUserOnline(2L);
            verify(chatHandler).sendToUser(eq(2L), messagePayloadCaptor.capture());

            Map<String, Object> sentMessage = messagePayloadCaptor.getValue();
            assertThat(sentMessage.get("type")).isEqualTo("PRIVATE_MESSAGE");
            assertThat(sentMessage.get("message")).isEqualTo(testMessageDto);
        }

        @Test
        @DisplayName("✅ Отправка в DLQ: получатель оффлайн")
        void sendsToDlqWhenRecipientOffline() throws Exception {
            String kafkaMessage = "private-message-json";
            String topic = "private-messages";

            when(messageConverter.convertToEvent(kafkaMessage)).thenReturn(privateMessageEvent);
            when(chatMessageService.sendPrivateMessage(any(MessageDto.class), eq(1L), eq(2L)))
                    .thenReturn(testMessage);
            when(chatHandler.isUserOnline(2L)).thenReturn(false);
            when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("dlq-payload");

            messageConsumer.consumeMessage(kafkaMessage, topic);

            verify(kafkaTemplate).send(eq("private-messages-dlq"), kafkaPayloadCaptor.capture());
            verify(chatHandler, never()).sendToUser(eq(2L), any());

            assertThat(kafkaPayloadCaptor.getValue()).isEqualTo("dlq-payload");
        }

        @Test
        @DisplayName("✅ Подтверждение доставки отправителю")
        void sendsDeliveryConfirmationToSender(){
            String kafkaMessage = "private-message-json";
            String topic = "private-messages";

            when(messageConverter.convertToEvent(kafkaMessage)).thenReturn(privateMessageEvent);
            when(chatMessageService.sendPrivateMessage(any(MessageDto.class), eq(1L), eq(2L)))
                    .thenReturn(testMessage);
            when(chatHandler.isUserOnline(2L)).thenReturn(true);
            when(messageConverter.convertToDto(testMessage)).thenReturn(testMessageDto);

            messageConsumer.consumeMessage(kafkaMessage, topic);

            verify(chatHandler).sendToUser(eq(1L), messagePayloadCaptor.capture());

            Map<String, Object> confirmation = messagePayloadCaptor.getAllValues().stream()
                    .filter(msg -> "DELIVERY_CONFIRMATION".equals(msg.get("type")))
                    .findFirst()
                    .orElse(null);

            assertThat(confirmation).isNotNull();
            assertThat(confirmation.get("messageId")).isEqualTo(999L);
            assertThat(confirmation.get("status")).isEqualTo("DELIVERED");
        }
    }

    @Nested
    @DisplayName("Обработка группового сообщения")
    class GroupMessage {

        @Test
        @DisplayName("✅ Рассылка онлайн-участникам")
        void deliversToOnlineMembers() {
            String kafkaMessage = "group-message-json";
            List<Long> chatMembers = List.of(1L, 2L, 3L);

            when(messageConverter.convertToEvent(kafkaMessage)).thenReturn(groupMessageEvent);
            when(chatMessageService.sendMessage(any(MessageDto.class), eq(1L))).thenReturn(testMessage);
            when(chatMessageService.getChatMembers(100L)).thenReturn(chatMembers);
            when(messageConverter.convertToDto(testMessage)).thenReturn(testMessageDto);
            when(chatHandler.isUserOnline(2L)).thenReturn(true);
            when(chatHandler.isUserOnline(3L)).thenReturn(true);

            messageConsumer.consumeMessage(kafkaMessage, "group-messages");

            verify(chatHandler).sendToUser(eq(2L), argThat(hasGroupMessageProperties()));
            verify(chatHandler).sendToUser(eq(3L), argThat(hasGroupMessageProperties()));
            verify(chatHandler, never()).sendToUser(eq(1L), any());
        }

        @Test
        @DisplayName("✅ Отправка в DLQ для оффлайн-участников")
        void sendsDlqForOfflineMembers() throws Exception {
            String kafkaMessage = "group-message-json";
            List<Long> chatMembers = List.of(1L, 2L, 3L, 4L);

            when(messageConverter.convertToEvent(kafkaMessage)).thenReturn(groupMessageEvent);
            when(chatMessageService.sendMessage(any(MessageDto.class), eq(1L))).thenReturn(testMessage);
            when(chatMessageService.getChatMembers(100L)).thenReturn(chatMembers);
            when(messageConverter.convertToDto(testMessage)).thenReturn(testMessageDto);
            when(chatHandler.isUserOnline(2L)).thenReturn(true);
            when(chatHandler.isUserOnline(3L)).thenReturn(true);
            when(chatHandler.isUserOnline(4L)).thenReturn(false);
            when(objectMapper.writeValueAsString(any(Map.class))).thenReturn("dlq-payload");

            messageConsumer.consumeMessage(kafkaMessage, "group-messages");

            verify(chatHandler).sendToUser(eq(2L), argThat(hasGroupMessageProperties()));
            verify(chatHandler).sendToUser(eq(3L), argThat(hasGroupMessageProperties()));
            verify(chatHandler, never()).sendToUser(eq(4L), any());
            verify(kafkaTemplate).send(eq("group-messages-dlq"), anyString());
            verify(chatHandler, times(2)).sendToUser(anyLong(), any(Map.class));
        }

        private ArgumentMatcher<Map<String, Object>> hasGroupMessageProperties() {
            return payload ->
                    "GROUP_MESSAGE".equals(payload.get("type")) &&
                            Long.valueOf(100L).equals(payload.get("chatId")) &&
                            testMessageDto.equals(payload.get("message"));
        }
    }

    @Nested
    @DisplayName("Обработка ошибок")
    class ErrorHandling {

        @Test
        @DisplayName("❌ Ошибка при конвертации сообщения")
        void throwsExceptionOnProcessingFailure() {
            String kafkaMessage = "invalid-message-json";
            String topic = "private-messages";

            when(messageConverter.convertToEvent(kafkaMessage))
                    .thenThrow(new RuntimeException("Conversion failed"));

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> messageConsumer.consumeMessage(kafkaMessage, topic));

            assertThat(exception.getMessage()).isEqualTo("Ошибка обработки Kafka сообщения");
            verify(chatMessageService, never()).sendPrivateMessage(any(), anyLong(), anyLong());
        }
    }
}