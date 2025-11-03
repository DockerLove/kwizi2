package com.example.kwizi.service;

import com.example.kwizi.DTO.internal.MessageDto;
import com.example.kwizi.DTO.response.ChatHistoryResponse;
import com.example.kwizi.exception.ChatNotFoundException;
import com.example.kwizi.exception.MessageService.MessageEditTimeExpiredException;
import com.example.kwizi.exception.MessageService.MessageNotFoundException;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.model.Chat;
import com.example.kwizi.model.ChatMember;
import com.example.kwizi.model.Message;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.ChatMemberRepository;
import com.example.kwizi.repository.ChatRepository;
import com.example.kwizi.repository.MessageRepository;
import com.example.kwizi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;


@Lazy
@Service
@Transactional(readOnly = true)
public class ChatMessageService implements ChatMessageServiceInterface {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageService.class);

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final ChatMemberRepository chatMemberRepository;

    @Autowired
    public ChatMessageService(MessageRepository messageRepository,
                              ChatRepository chatRepository,
                              UserRepository userRepository,
                              ChatMemberRepository chatMemberRepository) {
        this.messageRepository = messageRepository;
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.chatMemberRepository = chatMemberRepository;
    }

    @Transactional
    public Message sendMessage(MessageDto messageDto, Long senderId) {
        logger.info("Запрос на отправку сообщения. ID отправителя: {}, ID чата: {}",
                senderId, messageDto.getChatId());

        Long chatId = messageDto.getChatId();
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Чат с ID " + chatId + " не найден"));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с ID " + senderId + " не найден"));

        validateUserInChat(senderId, chatId);

        return createAndSaveMessage(chat, sender, messageDto.getText());
    }

    @Transactional
    public Chat getOrCreateChat(Long chatId, User creator) {
        logger.debug("Получение или создание чата. ID чата: {}, ID создателя: {}",
                chatId, creator.getId());

        Chat chat = chatRepository.findById(chatId)
                .orElseGet(() -> createNewChat(chatId, creator));

        if (chat.getCreatedBy() == null) {
            chat.setCreatedBy(creator);
            chat = chatRepository.save(chat);
            logger.info("Чат инициализирован создателем. ID чата: {}, ID создателя: {}",
                    chatId, creator.getId());
        }

        return chat;
    }

    @Transactional(readOnly = true)
    public Page<ChatHistoryResponse> getChatHistory(Long chatId, int page, int size, String sort, String username) {
        logger.debug("Получение истории чата. ID чата: {}, пользователь: {}", chatId, username);

        // Проверка доступа
        if (!chatMemberRepository.existsByChatIdAndUsername(chatId, username)) {
            logger.warn("Попытка доступа к чужому чату. ID чата: {}, пользователь: {}", chatId, username);
            throw new AccessDeniedException("Нет доступа к чату");
        }

        // Создание pageable
        Sort sorting = Sort.by(Sort.Order.desc("createdAt"));
        Pageable pageable = PageRequest.of(page, size, sorting);

        // Получение данных
        Page<Message> messages = messageRepository.findByChatId(chatId, pageable);

        logger.debug("Найдено сообщений в чате {}: {} из {}",
                chatId, messages.getNumberOfElements(), messages.getTotalElements());

        return messages.map(this::convertToChatHistoryResponse);
    }



    @Transactional
    public Message sendPrivateMessage(MessageDto messageDto, Long senderId, Long recipientId) {
        logger.info("Запрос на отправку личного сообщения. ID отправителя: {}, ID получателя: {}",
                senderId, recipientId);

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new UserNotFoundException("Отправитель с ID " + senderId + " не найден"));
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new UserNotFoundException("Получатель с ID " + recipientId + " не найден"));

        Chat chat = findOrCreatePrivateChat(senderId, recipientId, sender, recipient);

        return createAndSaveMessage(chat, sender, messageDto.getText());
    }

    @Transactional
    public void editMessage(Long messageId, String newText, String username) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Сообщение не найдено"));

        // Проверка прав
        if (!message.getSender().getUsername().equals(username)) {
            throw new AccessDeniedException("Вы можете редактировать только свои сообщения");
        }

        // Проверка времени (24 часа)
        OffsetDateTime twentyFourHoursAgo = OffsetDateTime.now().minusHours(24);
        if (message.getCreatedAt().isBefore(twentyFourHoursAgo)) {
            throw new MessageEditTimeExpiredException("Редактирование доступно только в течение 24 часов");
        }

        // Обновление
        message.edit(newText);
        messageRepository.save(message);

        logger.debug("Сообщение обновлено в БД. ID: {}, Новый текст: {} символов",
                messageId, newText.length());
    }

    public List<Long> getChatMembers(Long chatId) {
        logger.info("Запрос на получение участников чата. ID чата: {}", chatId);

        if (!chatRepository.existsById(chatId)) {
            throw new ChatNotFoundException("Чат с ID " + chatId + " не найден");
        }

        List<Long> members = chatMemberRepository.findUserIdsByChatId(chatId);
        logger.info("Получен список участников чата. ID чата: {}, количество участников: {}",
                chatId, members.size());

        return members;
    }

    private Message createAndSaveMessage(Chat chat, User sender, String text) {
        Message message = new Message();
        message.setChat(chat);
        message.setSender(sender);
        message.setText(text);
        message.setCreatedAt(OffsetDateTime.now());

        Message savedMessage = messageRepository.save(message);
        logger.info("Сообщение успешно отправлено. ID сообщения: {}, ID чата: {}",
                savedMessage.getId(), chat.getId());
        return savedMessage;
    }

    private Chat createNewChat(Long chatId, User creator) {
        Chat chat = new Chat();
        chat.setId(chatId);
        chat.setCreatedBy(creator);
        chat.setCreatedAt(OffsetDateTime.now());

        Chat savedChat = chatRepository.save(chat);
        logger.info("Создан новый чат. ID чата: {}, ID создателя: {}",
                chatId, creator.getId());
        return savedChat;
    }

    private Chat findOrCreatePrivateChat(Long senderId, Long recipientId, User sender, User recipient) {
        return chatMemberRepository.findPrivateChatIdByUserIds(senderId, recipientId)
                .map(chatId -> chatRepository.findById(chatId)
                        .orElseThrow(() -> new ChatNotFoundException("Чат не найден")))
                .orElseGet(() -> createPrivateChat(sender, recipient));
    }

    private Chat createPrivateChat(User sender, User recipient) {
        Chat chat = new Chat();
        chat.setCreatedBy(sender);
        chat.setCreatedAt(OffsetDateTime.now());
        Chat savedChat = chatRepository.save(chat);

        ChatMember chatMemberSender = new ChatMember(savedChat, sender);
        ChatMember chatMemberRecipient = new ChatMember(savedChat, recipient);

        chatMemberRepository.saveAll(List.of(chatMemberSender, chatMemberRecipient));
        logger.info("Создан новый приватный чат. ID чата: {}, участники: {}, {}",
                savedChat.getId(), sender.getId(), recipient.getId());

        return savedChat;
    }

    private void validateUserInChat(Long userId, Long chatId) {
        boolean isUserInChat = chatMemberRepository.existsByChatIdAndUserId(chatId, userId);
        if (!isUserInChat) {
            throw new SecurityException("Пользователь " + userId + " не является участником чата " + chatId);
        }
    }

    protected ChatHistoryResponse convertToChatHistoryResponse(Message message) {
        ChatHistoryResponse response = new ChatHistoryResponse();
        response.setMessageId(message.getId());
        response.setText(message.getText());
        response.setSenderId(message.getSender().getId());
        response.setSenderName(message.getSender().getUsername());
        response.setCreatedAt(message.getCreatedAt().toLocalDateTime());
        response.setUpdatedAt(message.getUpdatedAt());
        response.setEdited(message.isEdited());
        return response;
    }
}