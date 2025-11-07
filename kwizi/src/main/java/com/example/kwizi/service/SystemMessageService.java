package com.example.kwizi.service;

import com.example.kwizi.enums.MessageSystemType;
import com.example.kwizi.model.Chat;
import com.example.kwizi.model.Message;
import com.example.kwizi.repository.ChatRepository;
import com.example.kwizi.repository.MessageRepository;
import com.example.kwizi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@Transactional
public class SystemMessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final NotificationService notificationService;

    public SystemMessageService(MessageRepository messageRepository,
                                UserRepository userRepository,
                                ChatRepository chatRepository,
                                NotificationService notificationService) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.chatRepository = chatRepository;
        this.notificationService = notificationService;
    }

    /**
     * Пользователь добавлен в чат (админом или другим пользователем)
     */
    public void createUserAddedMessage(Chat chat, String addedUsername, String addedByUsername) {
        Message systemMessage = new Message();
        systemMessage.setChat(chat);
        systemMessage.setSender(userRepository.findByUsername(addedByUsername).orElseThrow());
        systemMessage.setMessageType(MessageSystemType.USER_ADDED);
        systemMessage.setText(String.format("Пользователь %s добавлен в чат пользователем %s",
                addedUsername, addedByUsername));
        systemMessage.setCreatedAt(OffsetDateTime.now());

        messageRepository.save(systemMessage);
    }

    /**
     * Пользователь удален из чата (админом)
     */
    public void createUserRemovedMessage(Chat chat, String removedUsername, String removedByUsername) {
        Message systemMessage = new Message();
        systemMessage.setChat(chat);
        systemMessage.setSender(userRepository.findByUsername(removedByUsername).orElseThrow());
        systemMessage.setMessageType(MessageSystemType.USER_REMOVED);
        systemMessage.setText(String.format("Пользователь %s удален из чата пользователем %s",
                removedUsername, removedByUsername));
        systemMessage.setCreatedAt(OffsetDateTime.now());

        messageRepository.save(systemMessage);
    }

    /**
     * Пользователь вышел из чата (добровольно)
     */
    public void createUserLeftMessage(Chat chat, String username) {
        Message systemMessage = new Message();
        systemMessage.setChat(chat);
        systemMessage.setSender(userRepository.findByUsername(username).orElseThrow());
        systemMessage.setMessageType(MessageSystemType.USER_LEFT);
        systemMessage.setText(String.format("Пользователь %s вышел из чата", username));
        systemMessage.setCreatedAt(OffsetDateTime.now());

        messageRepository.save(systemMessage);
    }

    /**
     * Название группы изменено
     */
    public void createGroupTitleChangedMessage(Chat chat, String oldTitle, String newTitle, String changedByUsername) {
        Message systemMessage = new Message();
        systemMessage.setChat(chat);
        systemMessage.setSender(userRepository.findByUsername(changedByUsername).orElseThrow());
        systemMessage.setMessageType(MessageSystemType.GROUP_TITLE_CHANGED);
        systemMessage.setText(String.format("Название группы изменено с \"%s\" на \"%s\"",
                oldTitle, newTitle));
        systemMessage.setCreatedAt(OffsetDateTime.now());

        messageRepository.save(systemMessage);
    }

    /**
     * Пользователь назначен администратором
     */
    public void createUserPromotedMessage(Chat chat, String targetUsername, String promotedByUsername) {
        Message systemMessage = new Message();
        systemMessage.setChat(chat);
        systemMessage.setSender(userRepository.findByUsername(promotedByUsername).orElseThrow());
        systemMessage.setMessageType(MessageSystemType.USER_PROMOTED);
        systemMessage.setText(String.format("Пользователь %s назначен администратором пользователем %s",
                targetUsername, promotedByUsername));
        systemMessage.setCreatedAt(OffsetDateTime.now());

        messageRepository.save(systemMessage);
    }

    /**
     * У пользователя забрали права администратора
     */
    public void createUserDemotedMessage(Chat chat, String targetUsername, String demotedByUsername) {
        Message systemMessage = new Message();
        systemMessage.setChat(chat);
        systemMessage.setSender(userRepository.findByUsername(demotedByUsername).orElseThrow());
        systemMessage.setMessageType(MessageSystemType.USER_DEMOTED);
        systemMessage.setText(String.format("Пользователь %s лишен прав администратора пользователем %s",
                targetUsername, demotedByUsername));
        systemMessage.setCreatedAt(OffsetDateTime.now());

        messageRepository.save(systemMessage);
    }
}
