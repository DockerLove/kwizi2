package com.example.kwizi.service;

import com.example.kwizi.websocket.UniversalChatHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}