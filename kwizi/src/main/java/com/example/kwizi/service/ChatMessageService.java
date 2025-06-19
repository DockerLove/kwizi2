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

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Lazy
public class ChatMessageService implements ChatMessageServiceInterface {
    //todo реальзовано создание чата но чату не присваивается id создателя и также нет проверки кто в каком чате находится
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
        try {
            // Получение chatId (комбинация ID отправителя и получателя)
            Long chatId = generateChatId(senderId, recipientId);
            System.out.println("ChatID: " + chatId);
            // Получение чата
            Chat chat = chatRepository.findById(chatId).orElse(null);

            // Получение отправителя и получателя
            User sender = userRepository.findById(senderId)
                    .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
            User recipient = userRepository.findById(recipientId)
                    .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));

            // Если чат не существует, создаем его и добавляем отправителя и получателя в chat_members
            if (chat == null) {
                System.out.println("Chat NULL");
                chat = getOrCreateChat(chatId, sender); // Создаем чат
                System.out.println("Chat after creating " + chat);
                // Добавляем отправителя и получателя в chat_members
                ChatMember chatMemberSender = new ChatMember(chat, sender);
                ChatMember chatMemberRecipient = new ChatMember(chat, recipient);

                chatMemberRepository.save(chatMemberSender);
                chatMemberRepository.save(chatMemberRecipient);
            } else {
                System.out.println("Chat not NULL");
            }

            // Создание сообщения
            Message message = new Message();
            message.setChat(chat);
            message.setSender(sender);
            message.setText(messageDto.getText());
            message.setCreatedAt(OffsetDateTime.now());

            // Сохранение сообщения
            Message savedMessage =  messageRepository.save(message);
            return savedMessage;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Long generateChatId(Long senderId, Long recipientId) {
        // Ensure IDs are not null
        if (senderId == null || recipientId == null) {
            throw new IllegalArgumentException("SenderId and RecipientId cannot be null");
        }

        // Ensure IDs are not equal, as it doesn't make sense for a user to send messages to themselves.
        if (senderId.equals(recipientId)) {
            throw new IllegalArgumentException("SenderId and RecipientId cannot be the same");
        }

        long chatId;

        // Ensure ChatId is created same every time irrespective of the order of sender and recipient.
        if (senderId < recipientId) {
            chatId = senderId * 10000 + recipientId;
        } else {
            chatId = recipientId * 10000 + senderId;
        }
        return chatId;
    }







}