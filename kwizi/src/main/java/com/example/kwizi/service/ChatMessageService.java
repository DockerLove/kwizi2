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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;


@Lazy
@Service
public class ChatMessageService implements ChatMessageServiceInterface {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageService.class);

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
        logger.info("Запрос на отправку сообщения. ID отправителя: {}, ID чата: {}", senderId, messageDto.getChatId());
        try {
            Long chatId = messageDto.getChatId();
            Chat chat = chatRepository.findById(chatId).orElse(null);
            if (chat == null) {
                throw new IllegalArgumentException("Чат не найден");
            }

            User sender = userRepository.findById(senderId)
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

            Message message = new Message();
            message.setChat(chat);
            message.setSender(sender);
            message.setText(messageDto.getText());
            message.setCreatedAt(OffsetDateTime.now());

            Message savedMessage = messageRepository.save(message);
            logger.info("Сообщение успешно отправлено. ID сообщения: {}, ID чата: {}", savedMessage.getId(), chatId);
            return savedMessage;

        }catch (IllegalArgumentException ex){
            throw new IllegalArgumentException(ex.getMessage());
        } catch (Exception e) {
            logger.error("Ошибка при отправке сообщения. ID отправителя: {}, сообщение: {}", senderId, messageDto.getText(), e);
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public Chat getOrCreateChat(Long chatId, User creator) {
        logger.debug("Получение или создание чата. ID чата: {}, ID создателя: {}", chatId, creator.getId());
        chatRepository.createChatIfNotExists(chatId); // Сначала убеждаемся, что чат существует
        Chat chat = chatRepository.findById(chatId).orElse(null);
        if (chat != null && chat.getCreatedBy() == null) { // Проверяем, установлен ли createdBy
            chat.setCreatedBy(creator);
            chat = chatRepository.save(chat); // Сохраняем чат с установленным createdBy
            logger.info("Чат создан и инициализирован. ID чата: {}, ID создателя: {}", chatId, creator.getId());
        } else {
            logger.debug("Чат уже существует или создан, createdBy уже установлен. ID чата: {}", chatId);
        }
        return chat;
    }

    public List<Message> getChatHistory(Long chatId) {
        logger.info("Запрос истории сообщений для чата. ID чата: {}", chatId);
        // Проверка существования чата
        chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Чат не найден"));
        logger.info("История сообщений для чата {} получена", chatId);
        return messageRepository.findByChatIdAndIsDeleted(chatId, false);
    }

    @Transactional
    public Message sendPrivateMessage(MessageDto messageDto, Long senderId, Long recipientId) {
        logger.info("Запрос на отправку личного сообщения. ID отправителя: {}, ID получателя: {}", senderId, recipientId);
        // 1. Проверяем существование чата
        Optional<Long> existingChatId = chatMemberRepository.findPrivateChatIdByUserIds(senderId, recipientId);

        Chat chat;
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Отправитель не найден"));
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new IllegalArgumentException("Получатель не найден"));

        if (existingChatId.isPresent()) {
            // 2. Если чат существует - получаем его
            chat = chatRepository.findById(existingChatId.get())
                    .orElseThrow(() -> new IllegalArgumentException("Чат не найден"));
            logger.debug("Найден существующий чат для личного сообщения. ID чата: {}", chat.getId());
        } else {
            // 3. Если чата нет - создаем новый
            chat = new Chat();
            chat.setCreatedBy(sender); // Устанавливаем создателя чата
            chat.setCreatedAt(OffsetDateTime.now());
            chat = chatRepository.save(chat);
            logger.info("Создан новый чат для личного сообщения. ID чата: {}", chat.getId());

            // 4. Добавляем участников
            ChatMember chatMemberSender = new ChatMember(chat, sender);
            ChatMember chatMemberRecipient = new ChatMember(chat, recipient);

            chatMemberRepository.saveAll(List.of(chatMemberSender, chatMemberRecipient));
            logger.debug("Добавлены участники в новый приватный чат. ID чата: {}, ID отправителя: {}, ID получателя: {}", chat.getId(), senderId, recipientId);
        }

        // 5. Создаем и сохраняем сообщение
        Message message = new Message();
        message.setChat(chat);
        message.setSender(sender);
        message.setText(messageDto.getText());
        message.setCreatedAt(OffsetDateTime.now());

        Message savedMessage = messageRepository.save(message);
        logger.info("Личное сообщение успешно отправлено. ID сообщения: {}, ID чата: {}", savedMessage.getId(), chat.getId());
        return messageRepository.save(message);
    }

    public List<Long> getChatMembers(Long chatId) {
        logger.info("Запрос на получение участников чата. ID чата: {}", chatId);
        List<Long> members = chatMemberRepository.findUserIdsByChatId(chatId);
        logger.info("Получен список участников чата. ID чата: {}, количество участников: {}", chatId, members.size());
        return members;
    }
}