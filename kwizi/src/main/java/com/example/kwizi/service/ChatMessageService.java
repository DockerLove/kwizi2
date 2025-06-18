package com.example.kwizi.service;
import com.example.kwizi.DTO.internal.MessageDto;
import com.example.kwizi.model.Chat;
import com.example.kwizi.model.Message;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.ChatMemberRepository;
import com.example.kwizi.repository.ChatRepository;
import com.example.kwizi.repository.MessageRepository;
import com.example.kwizi.repository.UserRepository;
import jakarta.persistence.LockModeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Lazy
public class ChatMessageService implements ChatMessageServiceInterface {

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final ChatMemberRepository chatMemberRepository;

    @Autowired
    public ChatMessageService(MessageRepository messageRepository, ChatRepository chatRepository, UserRepository userRepository, ChatMemberRepository chatMemberRepository) {
        this.messageRepository = messageRepository;
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.chatMemberRepository = chatMemberRepository;
    }

    @Transactional
    public Message sendMessage(MessageDto messageDto, Long senderId) {
        System.out.println("sendMessage - Starting transaction");

        try {
            Long chatId = messageDto.getChatId();
            System.out.println("sendMessage - Chat ID: " + chatId);

            Chat chat = chatRepository.findById(chatId).orElse(null);

            if (chat == null) {
                System.out.println("sendMessage - Chat not found, creating new chat with ID: " + chatId);
                chat = getOrCreateChat(chatId); // Используем отдельный метод с блокировкой
            }

            if (chat == null) {
                System.err.println("sendMessage - Chat not found for chatId: " + chatId);
                throw new IllegalArgumentException("Chat not found");
            }

            User sender = userRepository.findById(senderId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            Message message = new Message();
            message.setChat(chat);
            message.setSender(sender);
            message.setText(messageDto.getText());
            message.setCreatedAt(OffsetDateTime.now());

            System.out.println("sendMessage - Saving message");
            Message savedMessage = messageRepository.save(message);
            System.out.println("sendMessage - Message saved successfully");

            System.out.println("sendMessage - Committing transaction");
            return savedMessage;

        } catch (Exception e) {
            System.err.println("sendMessage - Transaction failed: " + e.getMessage());
            throw new RuntimeException(e); // Оборачиваем исключение в RuntimeException
        }
    }

    @Transactional
    public Chat getOrCreateChat(Long chatId) {
        chatRepository.createChatIfNotExists(chatId); // Атомарное создание (если нет)
        return chatRepository.findById(chatId).orElseThrow();
    }

    public List<Message> getChatHistory(Long chatId) {
        // Проверка существования чата
        chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));

        return messageRepository.findByChatIdAndIsDeleted(chatId, false); // Возвращаем только не удаленные сообщения
    }
}