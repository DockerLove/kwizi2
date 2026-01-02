package com.example.kwizi.service;

import com.example.kwizi.websocket.UniversalChatHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService тесты")
class NotificationServiceTest {

    @Mock
    private UniversalChatHandler chatHandler;

    @InjectMocks
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<String> payloadCaptor;

    @Captor
    private ArgumentCaptor<Long> chatIdCaptor;

    private final Long CHAT_ID = 1L;
    private final Long MESSAGE_ID = 100L;
    private final String USERNAME = "testuser";
    private final String ADMIN_USERNAME = "adminuser";

    private final Pattern TIMESTAMP_PATTERN = Pattern.compile(
            "\"timestamp\":\"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+Z\""
    );

    @Nested
    @DisplayName("Уведомления о сообщениях")
    class MessageNotificationsTests {

        @Test
        @DisplayName("Уведомление об изменении сообщения")
        void notifyMessageEdited_WithValidData_ShouldSendCorrectPayload() {
            
            String newText = "Updated message text";

            notificationService.notifyMessageEdited(CHAT_ID, MESSAGE_ID, newText, USERNAME);

            verify(chatHandler).broadcastToChat(chatIdCaptor.capture(), payloadCaptor.capture());

            assertThat(chatIdCaptor.getValue())
                    .as("Должен отправить в правильный чат")
                    .isEqualTo(CHAT_ID);

            assertThat(payloadCaptor.getValue())
                    .as("Должен отправить корректный JSON")
                    .isEqualTo(
                            "{\"type\":\"MESSAGE_EDITED\", \"data\":{\"messageId\":100, \"chatId\":1, \"newText\":\"Updated message text\", \"editedBy\":\"testuser\"}}"
                    );
        }

        @Test
        @DisplayName("Уведомление об удалении сообщения")
        void notifyMessageDeleted_WithValidData_ShouldSendCorrectPayload() {

            notificationService.notifyMessageDeleted(CHAT_ID, MESSAGE_ID, ADMIN_USERNAME);
            
            verify(chatHandler).broadcastToChat(chatIdCaptor.capture(), payloadCaptor.capture());

            String payload = payloadCaptor.getValue();

            assertThat(chatIdCaptor.getValue())
                    .as("Должен отправить в правильный чат")
                    .isEqualTo(CHAT_ID);

            assertThat(payload)
                    .as("Должен содержать правильный тип")
                    .contains("\"type\":\"MESSAGE_DELETED\"")
                    .as("Должен содержать ID сообщения")
                    .contains("\"messageId\":100")
                    .as("Должен содержать ID чата")
                    .contains("\"chatId\":1")
                    .as("Должен содержать кто удалил")
                    .contains("\"deletedBy\":\"adminuser\"")
                    .as("Должен содержать валидный timestamp")
                    .containsPattern(TIMESTAMP_PATTERN);
        }
    }

    @Nested
    @DisplayName("Уведомления о пользователях")
    class UserNotificationsTests {

        @Test
        @DisplayName("Уведомление о добавлении пользователя")
        void notifyUserAdded_WithValidData_ShouldSendCorrectPayload() {
            
            notificationService.notifyUserAdded(CHAT_ID, USERNAME, ADMIN_USERNAME);

            
            verify(chatHandler).broadcastToChat(chatIdCaptor.capture(), payloadCaptor.capture());

            String payload = payloadCaptor.getValue();

            assertThat(chatIdCaptor.getValue()).isEqualTo(CHAT_ID);
            assertThat(payload)
                    .contains("\"type\":\"USER_ADDED\"")
                    .contains("\"addedUsername\":\"testuser\"")
                    .contains("\"addedByUsername\":\"adminuser\"")
                    .containsPattern(TIMESTAMP_PATTERN);
        }

        @Test
        @DisplayName("Уведомление об удалении пользователя")
        void notifyUserRemoved_WithValidData_ShouldSendCorrectPayload() {
            
            notificationService.notifyUserRemoved(CHAT_ID, USERNAME, ADMIN_USERNAME);

            
            verify(chatHandler).broadcastToChat(chatIdCaptor.capture(), payloadCaptor.capture());

            String payload = payloadCaptor.getValue();

            assertThat(chatIdCaptor.getValue()).isEqualTo(CHAT_ID);
            assertThat(payload)
                    .contains("\"type\":\"USER_REMOVED\"")
                    .contains("\"removedUsername\":\"testuser\"")
                    .contains("\"removedByUsername\":\"adminuser\"")
                    .containsPattern(TIMESTAMP_PATTERN);
        }

        @Test
        @DisplayName("Уведомление о выходе пользователя")
        void notifyUserLeft_WithValidData_ShouldSendCorrectPayload() {
            
            notificationService.notifyUserLeft(CHAT_ID, USERNAME);

            
            verify(chatHandler).broadcastToChat(chatIdCaptor.capture(), payloadCaptor.capture());

            String payload = payloadCaptor.getValue();

            assertThat(chatIdCaptor.getValue()).isEqualTo(CHAT_ID);
            assertThat(payload)
                    .contains("\"type\":\"USER_LEFT\"")
                    .contains("\"username\":\"testuser\"")
                    .containsPattern(TIMESTAMP_PATTERN);
        }

        @Test
        @DisplayName("Уведомление о назначении администратора")
        void notifyUserPromoted_WithValidData_ShouldSendCorrectPayload() {
            
            notificationService.notifyUserPromoted(CHAT_ID, USERNAME, ADMIN_USERNAME);

            
            verify(chatHandler).broadcastToChat(chatIdCaptor.capture(), payloadCaptor.capture());

            String payload = payloadCaptor.getValue();

            assertThat(chatIdCaptor.getValue()).isEqualTo(CHAT_ID);
            assertThat(payload)
                    .contains("\"type\":\"USER_PROMOTED\"")
                    .contains("\"targetUsername\":\"testuser\"")
                    .contains("\"promotedByUsername\":\"adminuser\"")
                    .containsPattern(TIMESTAMP_PATTERN);
        }

        @Test
        @DisplayName("Уведомление о снятии прав администратора")
        void notifyUserDemoted_WithValidData_ShouldSendCorrectPayload() {
            
            notificationService.notifyUserDemoted(CHAT_ID, USERNAME, ADMIN_USERNAME);

            
            verify(chatHandler).broadcastToChat(chatIdCaptor.capture(), payloadCaptor.capture());

            String payload = payloadCaptor.getValue();

            assertThat(chatIdCaptor.getValue()).isEqualTo(CHAT_ID);
            assertThat(payload)
                    .contains("\"type\":\"USER_DEMOTED\"")
                    .contains("\"targetUsername\":\"testuser\"")
                    .contains("\"demotedByUsername\":\"adminuser\"")
                    .containsPattern(TIMESTAMP_PATTERN);
        }
    }

    @Nested
    @DisplayName("Уведомления о группах")
    class GroupNotificationsTests {

        @Test
        @DisplayName("Уведомление об изменении названия группы")
        void notifyGroupNameChanged_WithValidData_ShouldSendCorrectPayload() {
            
            String oldName = "Old Group Name";
            String newName = "New Group Name";
            
            notificationService.notifyGroupNameChanged(CHAT_ID, oldName, newName, ADMIN_USERNAME);

            verify(chatHandler).broadcastToChat(chatIdCaptor.capture(), payloadCaptor.capture());

            String payload = payloadCaptor.getValue();

            assertThat(chatIdCaptor.getValue()).isEqualTo(CHAT_ID);
            assertThat(payload)
                    .contains("\"type\":\"GROUP_NAME_CHANGED\"")
                    .contains("\"oldName\":\"Old Group Name\"")
                    .contains("\"newName\":\"New Group Name\"")
                    .contains("\"changedBy\":\"adminuser\"")
                    .containsPattern(TIMESTAMP_PATTERN);
        }

        @Test
        @DisplayName("Уведомление об изменении фотографии группы")
        void notifyGroupPhotoChanged_WithValidData_ShouldSendCorrectPayload() {
            
            notificationService.notifyGroupPhotoChanged(CHAT_ID, ADMIN_USERNAME);

            
            verify(chatHandler).broadcastToChat(chatIdCaptor.capture(), payloadCaptor.capture());

            String payload = payloadCaptor.getValue();

            assertThat(chatIdCaptor.getValue()).isEqualTo(CHAT_ID);
            assertThat(payload)
                    .contains("\"type\":\"GROUP_PHOTO_CHANGED\"")
                    .contains("\"changedBy\":\"adminuser\"")
                    .containsPattern(TIMESTAMP_PATTERN);
        }
    }

    @Nested
    @DisplayName("Обработка специальных символов в тексте")
    class SpecialCharactersHandlingTests {

        @Test
        @DisplayName("Текст с кавычками корректно форматируется")
        void notifyMessageEdited_WithQuotesInText_ShouldEscapeCorrectly() {
            
            String newText = "Message with \"quotes\" inside";
            
            notificationService.notifyMessageEdited(CHAT_ID, MESSAGE_ID, newText, USERNAME);

            
            verify(chatHandler).broadcastToChat(chatIdCaptor.capture(), payloadCaptor.capture());

            assertThat(payloadCaptor.getValue())
                    .as("Должен корректно обрабатывать кавычки в тексте")
                    .contains("\"newText\":\"Message with \"quotes\" inside\"");
        }

        @Test
        @DisplayName("Username со спецсимволами корректно форматируется")
        void notifyUserAdded_WithSpecialCharactersInUsername_ShouldHandleCorrectly() {
            
            String specialUsername = "user-name.test";

            
            notificationService.notifyUserAdded(CHAT_ID, specialUsername, ADMIN_USERNAME);

            
            verify(chatHandler).broadcastToChat(chatIdCaptor.capture(), payloadCaptor.capture());

            String payload = payloadCaptor.getValue();

            assertThat(payload)
                    .as("Должен корректно обрабатывать username со спецсимволами")
                    .contains("\"addedUsername\":\"user-name.test\"");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Пустой текст сообщения корректно обрабатывается")
        void notifyMessageEdited_WithEmptyText_ShouldHandleCorrectly(String newText) {

            notificationService.notifyMessageEdited(CHAT_ID, MESSAGE_ID, newText, USERNAME);
            
            verify(chatHandler).broadcastToChat(chatIdCaptor.capture(), payloadCaptor.capture());

            String payload = payloadCaptor.getValue();

            if (newText == null) {
                assertThat(payload).contains("\"newText\":\"null\"");
            } else {
                assertThat(payload).contains("\"newText\":\"\"");
            }
        }
    }

}