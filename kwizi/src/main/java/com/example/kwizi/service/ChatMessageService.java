package com.example.kwizi.service;
import com.example.kwizi.DTO.internal.MessageDto;
import com.example.kwizi.model.Chat;
import com.example.kwizi.model.ChatMember;
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

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

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
        try {
            Long chatId = messageDto.getChatId();
            Chat chat = chatRepository.findById(chatId).orElse(null);
            if (chat == null) {
                User creator = userRepository.findById(senderId)
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));
                chat = getOrCreateChat(chatId, creator); // Передаем пользователя, создающего чат
            }
            if (chat == null) {
                throw new IllegalArgumentException("Chat not found");
            }

            User sender = userRepository.findById(senderId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            Message message = new Message();
            message.setChat(chat);
            message.setSender(sender);
            message.setText(messageDto.getText());
            message.setCreatedAt(OffsetDateTime.now());

            Message savedMessage = messageRepository.save(message);
            return savedMessage;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public Chat getOrCreateChat(Long chatId, User creator) {
        chatRepository.createChatIfNotExists(chatId); // Сначала убеждаемся, что чат существует
        Chat chat = chatRepository.findById(chatId).orElse(null);
        if (chat != null && chat.getCreatedBy() == null) { // Проверяем, установлен ли createdBy
            chat.setCreatedBy(creator);
            chat = chatRepository.save(chat); // Сохраняем чат с установленным createdBy
        }
        return chat;
    }

    public List<Message> getChatHistory(Long chatId) {
        // Проверка существования чата
        chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));

        return messageRepository.findByChatIdAndIsDeleted(chatId, false);
    }

    @Transactional
    public Message sendPrivateMessage(MessageDto messageDto, Long senderId, Long recipientId) {
        // 1. Проверяем существование чата
        Optional<Long> existingChatId = chatMemberRepository.findPrivateChatIdByUserIds(senderId, recipientId);

        Chat chat;
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));

        if (existingChatId.isPresent()) {
            // 2. Если чат существует - получаем его
            chat = chatRepository.findById(existingChatId.get())
                    .orElseThrow(() -> new IllegalArgumentException("Chat not found"));
        } else {
            // 3. Если чата нет - создаем новый
            chat = new Chat();
            chat.setCreatedBy(sender); // Устанавливаем создателя чата
            chat.setCreatedAt(OffsetDateTime.now());
            chat = chatRepository.save(chat);

            // 4. Добавляем участников
            ChatMember chatMemberSender = new ChatMember(chat, sender);
            ChatMember chatMemberRecipient = new ChatMember(chat, recipient);

            chatMemberRepository.saveAll(List.of(chatMemberSender, chatMemberRecipient));
        }

        // 5. Создаем и сохраняем сообщение
        Message message = new Message();
        message.setChat(chat);
        message.setSender(sender);
        message.setText(messageDto.getText());
        message.setCreatedAt(OffsetDateTime.now());

        return messageRepository.save(message);
    }

}