package com.example.kwizi.service;

import com.example.kwizi.DTO.internal.MessageDto;
import com.example.kwizi.DTO.response.ChatHistoryResponse;
import com.example.kwizi.enums.ChatRole;
import com.example.kwizi.enums.ChatType;
import com.example.kwizi.exception.ChatNotFoundException;
import com.example.kwizi.exception.ChatService.ChatMemberNotFoundException;
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
    private final NotificationService notificationService;
    private final ChatService chatService;
    private final SystemMessageService systemMessageService;
    private final ChatMemberRepository chatMemberRepository;

    @Autowired
    public ChatMessageService(MessageRepository messageRepository,
                              ChatRepository chatRepository,
                              UserRepository userRepository,
                              ChatMemberRepository chatMemberRepository,
                              NotificationService notificationService,
                              SystemMessageService systemMessageService,
                              ChatService chatService) {
        this.messageRepository = messageRepository;
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.chatMemberRepository = chatMemberRepository;
        this.notificationService = notificationService;
        this.systemMessageService = systemMessageService;
        this.chatService = chatService;
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

        Message message = createAndSaveMessage(chat, sender, messageDto.getText());

        chatService.updateChatActivity(chatId);

        return message;
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

        Message message = createAndSaveMessage(chat, sender, messageDto.getText());

        chatService.updateChatActivity(chat.getId());

        return message;
    }

    @Transactional(readOnly = true)
    public Page<ChatHistoryResponse> getChatHistory(Long chatId, int page, int size, String sort, String username) {
        logger.debug("Получение истории чата. ID чата: {}, пользователь: {}", chatId, username);

        if (!chatMemberRepository.existsByChatIdAndUsername(chatId, username)) {
            logger.warn("Попытка доступа к чужому чату. ID чата: {}, пользователь: {}", chatId, username);
            throw new AccessDeniedException("Нет доступа к чату");
        }

        Sort sorting = Sort.by(Sort.Order.desc("createdAt"));
        Pageable pageable = PageRequest.of(page, size, sorting);

        Page<Message> messages = messageRepository.findByChatId(chatId, pageable);

        logger.debug("Найдено сообщений в чате {}: {} из {}",
                chatId, messages.getNumberOfElements(), messages.getTotalElements());

        return messages.map(this::convertToChatHistoryResponse);
    }

    @Transactional
    public void editMessage(Long messageId, String newText, String username) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Сообщение не найдено"));

        if (!message.getSender().getUsername().equals(username)) {
            throw new AccessDeniedException("Вы можете редактировать только свои сообщения");
        }

        OffsetDateTime twentyFourHoursAgo = OffsetDateTime.now().minusHours(24);
        if (message.getCreatedAt().isBefore(twentyFourHoursAgo)) {
            throw new MessageEditTimeExpiredException("Редактирование доступно только в течение 24 часов");
        }

        message.edit(newText);
        messageRepository.save(message);

        notificationService.notifyMessageEdited(
                message.getChat().getId(),
                messageId,
                newText,
                username
        );

        logger.debug("Сообщение обновлено в БД. ID: {}, Новый текст: {} символов",
                messageId, newText.length());
    }

    @Transactional
    public void deleteMessage(Long messageId, String username) {
        logger.info("Попытка удаления сообщения ID: {} пользователем: {}", messageId, username);

        User user = findUserByUsername(username);
        Message message = findMessageById(messageId);

        validateDeletePermissions(message, user.getId());

        notificationService.notifyMessageDeleted(message.getChat().getId(), messageId, username);

        messageRepository.delete(message);
        logger.info("Сообщение ID: {} успешно удалено пользователем ID: {}", messageId, user.getId());
    }

    private void validateDeletePermissions(Message message, Long userId) {
        Long chatId = message.getChat().getId();

        ChatMember requester = findChatMember(chatId, userId);
        logger.debug("Пользователь ID: {} является участником чата ID: {}", userId, chatId);

        if (isMessageSender(message, userId)) {
            logger.info("Пользователь ID: {} удаляет своё сообщение ID: {}", userId, message.getId());
            return;
        }

        if ((requester.isAdmin()) || (requester.isAdmin())) {
            logger.info("Админ или Владелец ID: {} удаляет сообщение ID: {} в чате ID: {}",
                    userId, message.getId(), chatId);
            return;
        }

        logger.warn("Доступ запрещён - попытка удаления чужого сообщения без прав. Сообщение ID: {}, пользователь ID: {}",
                message.getId(), userId);
        throw new AccessDeniedException("Недостаточно прав для удаления этого сообщения");
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.warn("Пользователь не найден: {}", username);
                    return new UserNotFoundException("Пользователь не найден");
                });
    }

    private Message findMessageById(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> {
                    logger.warn("Сообщение не найдено ID: {}", messageId);
                    return new MessageNotFoundException("Сообщение не найдено");
                });
    }

    private boolean isMessageSender(Message message, Long userId) {
        return message.getSender().getId().equals(userId);
    }

    private ChatMember findChatMember(Long chatId, Long userId) {
        logger.debug("Поиск участника чата - chatId: {}, userId: {}", chatId, userId);
        return chatMemberRepository.findByChatIdAndUserId(chatId, userId)
                .orElseThrow(() -> {
                    logger.warn("Участник чата не найден - chatId: {}, userId: {}", chatId, userId);
                    throw new ChatMemberNotFoundException("Вы не являетесь участником этого чата");
                });
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

    private Chat findOrCreatePrivateChat(Long senderId, Long recipientId, User sender, User recipient) {
        return chatMemberRepository.findPrivateChatIdByUserIds(senderId, recipientId)
                .map(chatId -> chatRepository.findById(chatId)
                        .orElseThrow(() -> new ChatNotFoundException("Чат не найден")))
                .orElseGet(() -> createPrivateChat(sender, recipient));
    }

    private Chat createPrivateChat(User sender, User recipient) {
        Chat chat = new Chat();
        chat.setChatType(ChatType.PRIVATE);
        chat.setCreatedAt(OffsetDateTime.now());
        chat.setLastActivityAt(OffsetDateTime.now());
        Chat savedChat = chatRepository.save(chat);

        ChatMember chatMemberSender = new ChatMember(savedChat, sender, ChatRole.MEMBER);
        ChatMember chatMemberRecipient = new ChatMember(savedChat, recipient, ChatRole.MEMBER);

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