package com.example.kwizi.service;

import com.example.kwizi.enums.MessageSystemType;
import com.example.kwizi.model.Chat;
import com.example.kwizi.model.Message;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.MessageRepository;
import com.example.kwizi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
@Transactional
public class SystemMessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final Map<MessageSystemType, String> messageTemplates;

    public SystemMessageService(MessageRepository messageRepository,
                                UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.messageTemplates = createTemplates();
        validateTemplates();
    }

    private Map<MessageSystemType, String> createTemplates() {
        return Map.of(
                MessageSystemType.USER_ADDED, "Пользователь %s добавлен в чат пользователем %s",
                MessageSystemType.USER_REMOVED, "Пользователь %s удален из чата пользователем %s",
                MessageSystemType.USER_LEFT, "Пользователь %s вышел из чата",
                MessageSystemType.USER_PROMOTED, "Пользователь %s назначен администратором пользователем %s",
                MessageSystemType.USER_DEMOTED, "Пользователь %s лишен прав администратора пользователем %s",
                MessageSystemType.GROUP_TITLE_CHANGED, "Пользователь %s изменил название группы с \"%s\" на \"%s\"",
                MessageSystemType.GROUP_PHOTO_CHANGED, "Пользователь %s изменил фотографию группы"
        );
    }

    private void validateTemplates() {
        for (MessageSystemType type : MessageSystemType.values()) {
            if (!messageTemplates.containsKey(type) && type != MessageSystemType.REGULAR) {
                throw new IllegalStateException("Шаблон для типа сообщения не найден: " + type);
            }
        }
    }

    public void createUserAddedMessage(Chat chat, String addedUsername, String addedByUsername) {
        createSystemMessage(chat, addedByUsername, MessageSystemType.USER_ADDED, addedUsername, addedByUsername);
    }

    public void createUserRemovedMessage(Chat chat, String removedUsername, String removedByUsername) {
        createSystemMessage(chat, removedByUsername, MessageSystemType.USER_REMOVED, removedUsername, removedByUsername);
    }

    public void createUserLeftMessage(Chat chat, String username) {
        createSystemMessage(chat, username, MessageSystemType.USER_LEFT, username);
    }

    public void createUserPromotedMessage(Chat chat, String targetUsername, String promotedByUsername) {
        createSystemMessage(chat, promotedByUsername, MessageSystemType.USER_PROMOTED, targetUsername, promotedByUsername);
    }

    public void createUserDemotedMessage(Chat chat, String targetUsername, String demotedByUsername) {
        createSystemMessage(chat, demotedByUsername, MessageSystemType.USER_DEMOTED, targetUsername, demotedByUsername);
    }

    public void createGroupNameChangedMessage(Chat chat, String oldName, String newName, String changedByUsername) {
        createSystemMessage(chat, changedByUsername, MessageSystemType.GROUP_TITLE_CHANGED, changedByUsername, oldName, newName);
    }

    public void createGroupPhotoChangedMessage(Chat chat, String changedByUsername) {
        createSystemMessage(chat, changedByUsername, MessageSystemType.GROUP_PHOTO_CHANGED, changedByUsername);
    }

    private void createSystemMessage(Chat chat, String senderUsername,
                                     MessageSystemType messageType, Object... templateArgs) {
        String template = messageTemplates.get(messageType);
        if (template == null) {
            throw new IllegalArgumentException("Шаблон для типа сообщения не найден: " + messageType);
        }

        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + senderUsername));

        String messageText = String.format(template, templateArgs);

        Message systemMessage = new Message();
        systemMessage.setChat(chat);
        systemMessage.setSender(sender);
        systemMessage.setMessageType(messageType);
        systemMessage.setText(messageText);
        systemMessage.setCreatedAt(OffsetDateTime.now());

        messageRepository.save(systemMessage);
    }

    public String getTemplate(MessageSystemType messageType) {
        return messageTemplates.get(messageType);
    }
}