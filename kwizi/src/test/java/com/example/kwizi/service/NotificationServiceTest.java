package com.example.kwizi.service;

import com.example.kwizi.websocket.UniversalChatHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
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
    private final String ANOTHER_USERNAME = "anotheruser";
    private final String ADMIN_USERNAME = "adminuser";

    // ===== ТЕСТЫ ДЛЯ УВЕДОМЛЕНИЙ О СООБЩЕНИЯХ =====

    @Test
    void notifyMessageEdited_WithValidData_ShouldSendCorrectPayload() {
        // Arrange
        String newText = "Updated message text";
        String editedBy = USERNAME;

        // Act
        notificationService.notifyMessageEdited(CHAT_ID, MESSAGE_ID, newText, editedBy);

        // Assert
        verify(chatHandler).broadcastToChat(chatIdCaptor.capture(), payloadCaptor.capture());

        assertThat(chatIdCaptor.getValue()).isEqualTo(CHAT_ID);
        assertThat(payloadCaptor.getValue()).isEqualTo(
                "{\"type\":\"MESSAGE_EDITED\", \"data\":{\"messageId\":100, \"chatId\":1, \"newText\":\"Updated message text\", \"editedBy\":\"testuser\"}}"
        );
    }

    @Test
    void notifyMessageDeleted_WithValidData_ShouldSendCorrectPayload() {
        // Arrange
        String deletedBy = ADMIN_USERNAME;

        // Act
        notificationService.notifyMessageDeleted(CHAT_ID, MESSAGE_ID, deletedBy);

        // Assert
        verify(chatHandler).broadcastToChat(chatIdCaptor.capture(), payloadCaptor.capture());

        String payload = payloadCaptor.getValue();
        assertThat(chatIdCaptor.getValue()).isEqualTo(CHAT_ID);
        assertThat(payload).contains("\"type\":\"MESSAGE_DELETED\"");
        assertThat(payload).contains("\"messageId\":100");
        assertThat(payload).contains("\"chatId\":1");
        assertThat(payload).contains("\"deletedBy\":\"adminuser\"");

        // Правильное регулярное выражение
        assertThat(payload).containsPattern("\"timestamp\":\"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+Z\"");
    }

    // ===== ТЕСТЫ ДЛЯ УВЕДОМЛЕНИЙ О ПОЛЬЗОВАТЕЛЯХ =====

    @Test
    void notifyUserAdded_WithValidData_ShouldSendCorrectPayload() {
        // Act
        notificationService.notifyUserAdded(CHAT_ID, USERNAME, ADMIN_USERNAME);

        // Assert
        verify(chatHandler).broadcastToChat(chatIdCaptor.capture(), payloadCaptor.capture());

        String payload = payloadCaptor.getValue();
        assertThat(chatIdCaptor.getValue()).isEqualTo(CHAT_ID);
        assertThat(payload).contains("\"type\":\"USER_ADDED\"");
        assertThat(payload).contains("\"addedUsername\":\"testuser\"");
        assertThat(payload).contains("\"addedByUsername\":\"adminuser\"");
        assertThat(payload).contains("\"timestamp\":");
    }

    @Test
    void notifyUserRemoved_WithValidData_ShouldSendCorrectPayload() {
        // Act
        notificationService.notifyUserRemoved(CHAT_ID, USERNAME, ADMIN_USERNAME);

        // Assert
        verify(chatHandler).broadcastToChat(chatIdCaptor.capture(), payloadCaptor.capture());

        String payload = payloadCaptor.getValue();
        assertThat(chatIdCaptor.getValue()).isEqualTo(CHAT_ID);
        assertThat(payload).contains("\"type\":\"USER_REMOVED\"");
        assertThat(payload).contains("\"removedUsername\":\"testuser\"");
        assertThat(payload).contains("\"removedByUsername\":\"adminuser\"");
        assertThat(payload).contains("\"timestamp\":");
    }

    @Test
    void notifyUserLeft_WithValidData_ShouldSendCorrectPayload() {
        // Act
        notificationService.notifyUserLeft(CHAT_ID, USERNAME);

        // Assert
        verify(chatHandler).broadcastToChat(chatIdCaptor.capture(), payloadCaptor.capture());

        String payload = payloadCaptor.getValue();
        assertThat(chatIdCaptor.getValue()).isEqualTo(CHAT_ID);
        assertThat(payload).contains("\"type\":\"USER_LEFT\"");
        assertThat(payload).contains("\"username\":\"testuser\"");
        assertThat(payload).contains("\"timestamp\":");
    }

    @Test
    void notifyUserPromoted_WithValidData_ShouldSendCorrectPayload() {
        // Act
        notificationService.notifyUserPromoted(CHAT_ID, USERNAME, ADMIN_USERNAME);

        // Assert
        verify(chatHandler).broadcastToChat(chatIdCaptor.capture(), payloadCaptor.capture());

        String payload = payloadCaptor.getValue();
        assertThat(chatIdCaptor.getValue()).isEqualTo(CHAT_ID);
        assertThat(payload).contains("\"type\":\"USER_PROMOTED\"");
        assertThat(payload).contains("\"targetUsername\":\"testuser\"");
        assertThat(payload).contains("\"promotedByUsername\":\"adminuser\"");
        assertThat(payload).contains("\"timestamp\":");
    }

    @Test
    void notifyUserDemoted_WithValidData_ShouldSendCorrectPayload() {
        // Act
        notificationService.notifyUserDemoted(CHAT_ID, USERNAME, ADMIN_USERNAME);

        // Assert
        verify(chatHandler).broadcastToChat(chatIdCaptor.capture(), payloadCaptor.capture());

        String payload = payloadCaptor.getValue();
        assertThat(chatIdCaptor.getValue()).isEqualTo(CHAT_ID);
        assertThat(payload).contains("\"type\":\"USER_DEMOTED\"");
        assertThat(payload).contains("\"targetUsername\":\"testuser\"");
        assertThat(payload).contains("\"demotedByUsername\":\"adminuser\"");
        assertThat(payload).contains("\"timestamp\":");
    }

    // ===== ТЕСТЫ ДЛЯ УВЕДОМЛЕНИЙ О ГРУППАХ =====

    @Test
    void notifyGroupNameChanged_WithValidData_ShouldSendCorrectPayload() {
        // Arrange
        String oldName = "Old Group Name";
        String newName = "New Group Name";
        String changedBy = ADMIN_USERNAME;

        // Act
        notificationService.notifyGroupNameChanged(CHAT_ID, oldName, newName, changedBy);

        // Assert
        verify(chatHandler).broadcastToChat(chatIdCaptor.capture(), payloadCaptor.capture());

        String payload = payloadCaptor.getValue();
        assertThat(chatIdCaptor.getValue()).isEqualTo(CHAT_ID);
        assertThat(payload).contains("\"type\":\"GROUP_NAME_CHANGED\"");
        assertThat(payload).contains("\"oldName\":\"Old Group Name\"");
        assertThat(payload).contains("\"newName\":\"New Group Name\"");
        assertThat(payload).contains("\"changedBy\":\"adminuser\"");
        assertThat(payload).contains("\"timestamp\":");
    }

    @Test
    void notifyGroupPhotoChanged_WithValidData_ShouldSendCorrectPayload() {
        // Act
        notificationService.notifyGroupPhotoChanged(CHAT_ID, ADMIN_USERNAME);

        // Assert
        verify(chatHandler).broadcastToChat(chatIdCaptor.capture(), payloadCaptor.capture());

        String payload = payloadCaptor.getValue();
        assertThat(chatIdCaptor.getValue()).isEqualTo(CHAT_ID);
        assertThat(payload).contains("\"type\":\"GROUP_PHOTO_CHANGED\"");
        assertThat(payload).contains("\"changedBy\":\"adminuser\"");
        assertThat(payload).contains("\"timestamp\":");
    }

    // ===== ТЕСТЫ ДЛЯ ГРАНИЧНЫХ СЛУЧАЕВ =====

    @Test
    void notifyMessageEdited_WithSpecialCharacters_ShouldHandleCorrectly() {
        // Arrange
        String newText = "Message with \"quotes\" and 'apostrophes'";
        String editedBy = "user-name.test";

        // Act
        notificationService.notifyMessageEdited(CHAT_ID, MESSAGE_ID, newText, editedBy);

        // Assert
        verify(chatHandler).broadcastToChat(chatIdCaptor.capture(), payloadCaptor.capture());

        String payload = payloadCaptor.getValue();
        assertThat(payload).contains("\"newText\":\"Message with \"quotes\" and 'apostrophes'\"");
        assertThat(payload).contains("\"editedBy\":\"user-name.test\"");
    }

    @Test
    void notifyMessageEdited_WithEmptyText_ShouldHandleCorrectly() {
        // Arrange
        String newText = "";
        String editedBy = USERNAME;

        // Act
        notificationService.notifyMessageEdited(CHAT_ID, MESSAGE_ID, newText, editedBy);

        // Assert
        verify(chatHandler).broadcastToChat(chatIdCaptor.capture(), payloadCaptor.capture());

        String payload = payloadCaptor.getValue();
        assertThat(payload).contains("\"newText\":\"\"");
    }

    @Test
    void notifyGroupNameChanged_WithLongNames_ShouldHandleCorrectly() {
        // Arrange
        String oldName = "Очень длинное название группы которое может быть очень длинным";
        String newName = "Еще более длинное новое название группы";
        String changedBy = USERNAME;

        // Act
        notificationService.notifyGroupNameChanged(CHAT_ID, oldName, newName, changedBy);

        // Assert
        verify(chatHandler).broadcastToChat(chatIdCaptor.capture(), payloadCaptor.capture());

        String payload = payloadCaptor.getValue();
        assertThat(payload).contains("\"oldName\":\"Очень длинное название группы которое может быть очень длинным\"");
        assertThat(payload).contains("\"newName\":\"Еще более длинное новое название группы\"");
    }
}