package com.example.kwizi.service;

import com.example.kwizi.model.Message;
import com.example.kwizi.websocket.UniversalChatHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class NotificationService {

    private final UniversalChatHandler chatHandler;

    @Autowired
    public NotificationService(UniversalChatHandler chatHandler) {
        this.chatHandler = chatHandler;
    }

    public void notifyMessageEdited(Long chatId, Long messageId, String newText, String editedBy) {
        String payload = String.format(
                "{\"type\":\"MESSAGE_EDITED\", \"data\":{\"messageId\":%d, \"chatId\":%d, \"newText\":\"%s\", \"editedBy\":\"%s\"}}",
                messageId, chatId, newText, editedBy
        );
        chatHandler.broadcastToChat(chatId, payload);
    }

    public void notifyMessageDeleted(Long chatId, Long messageId, String deletedBy) {
        String payload = String.format(
                "{\"type\":\"MESSAGE_DELETED\", \"data\":{\"messageId\":%d, \"chatId\":%d, \"deletedBy\":\"%s\", \"timestamp\":\"%s\"}}",
                messageId, chatId, deletedBy, Instant.now().toString()
        );
        chatHandler.broadcastToChat(chatId, payload);
    }

    public void notifyUserAdded(Long chatId, String addedUsername, String addedByUsername) {
        String payload = String.format(
                "{\"type\":\"USER_ADDED\", \"data\":{\"chatId\":%d, \"addedUsername\":\"%s\", \"addedByUsername\":\"%s\", \"timestamp\":\"%s\"}}",
                chatId, addedUsername, addedByUsername, Instant.now().toString()
        );
        chatHandler.broadcastToChat(chatId, payload);
    }

    public void notifyUserRemoved(Long chatId, String removedUsername, String removedByUsername) {
        String payload = String.format(
                "{\"type\":\"USER_REMOVED\", \"data\":{\"chatId\":%d, \"removedUsername\":\"%s\", \"removedByUsername\":\"%s\", \"timestamp\":\"%s\"}}",
                chatId, removedUsername, removedByUsername, Instant.now().toString()
        );
        chatHandler.broadcastToChat(chatId, payload);
    }

    public void notifyUserLeft(Long chatId, String username) {
        String payload = String.format(
                "{\"type\":\"USER_LEFT\", \"data\":{\"chatId\":%d, \"username\":\"%s\", \"timestamp\":\"%s\"}}",
                chatId, username, Instant.now().toString()
        );
        chatHandler.broadcastToChat(chatId, payload);
    }

    public void notifyGroupTitleChanged(Long chatId, String oldTitle, String newTitle, String changedByUsername) {
        String payload = String.format(
                "{\"type\":\"GROUP_TITLE_CHANGED\", \"data\":{\"chatId\":%d, \"oldTitle\":\"%s\", \"newTitle\":\"%s\", \"changedByUsername\":\"%s\", \"timestamp\":\"%s\"}}",
                chatId, oldTitle, newTitle, changedByUsername, Instant.now().toString()
        );
        chatHandler.broadcastToChat(chatId, payload);
    }

    public void notifyUserPromoted(Long chatId, String targetUsername, String promotedByUsername) {
        String payload = String.format(
                "{\"type\":\"USER_PROMOTED\", \"data\":{\"chatId\":%d, \"targetUsername\":\"%s\", \"promotedByUsername\":\"%s\", \"timestamp\":\"%s\"}}",
                chatId, targetUsername, promotedByUsername, Instant.now().toString()
        );
        chatHandler.broadcastToChat(chatId, payload);
    }

    public void notifyUserDemoted(Long chatId, String targetUsername, String demotedByUsername) {
        String payload = String.format(
                "{\"type\":\"USER_DEMOTED\", \"data\":{\"chatId\":%d, \"targetUsername\":\"%s\", \"demotedByUsername\":\"%s\", \"timestamp\":\"%s\"}}",
                chatId, targetUsername, demotedByUsername, Instant.now().toString()
        );
        chatHandler.broadcastToChat(chatId, payload);
    }

    // Дополнительный метод для уведомления о новом сообщении (для системных сообщений)
    public void notifyNewSystemMessage(Long chatId, Message systemMessage) {
        String payload = String.format(
                "{\"type\":\"NEW_SYSTEM_MESSAGE\", \"data\":{\"messageId\":%d, \"chatId\":%d, \"text\":\"%s\", \"messageType\":\"%s\", \"sender\":\"%s\", \"timestamp\":\"%s\"}}",
                systemMessage.getId(), chatId, systemMessage.getText(), systemMessage.getMessageType(),
                systemMessage.getSender().getUsername(), Instant.now().toString()
        );
        chatHandler.broadcastToChat(chatId, payload);
    }
}