package com.example.kwizi.service;

import com.example.kwizi.DTO.request.AddChatMemberRequestDto;
import com.example.kwizi.DTO.request.CreateGroupChatRequest;
import com.example.kwizi.DTO.request.CreatePrivateChatRequest;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.model.Chat;
import com.example.kwizi.model.ChatMember;
import com.example.kwizi.model.ChatMember.ChatMemberId;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.ChatMemberRepository;
import com.example.kwizi.repository.ChatRepository;
import com.example.kwizi.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final UserRepository userRepository;

    @Autowired
    public ChatService(ChatRepository chatRepository, ChatMemberRepository chatMemberRepository, UserRepository userRepository) {
        this.chatRepository = chatRepository;
        this.chatMemberRepository = chatMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void createGroupChat(CreateGroupChatRequest createChatRequestDto, String creatorUsername) {
        logger.info("Создание группового чата пользователем: {}", creatorUsername);

        Set<Long> uniqueUserIds = new HashSet<>(createChatRequestDto.getInitialMemberIds());
        if (uniqueUserIds.size() < createChatRequestDto.getInitialMemberIds().size()) {
            logger.warn("Обнаружены дубликаты ID пользователей в списке участников чата");
            throw new IllegalArgumentException("Список участников содержит дубликаты");
        }
        User creator = userRepository.findByUsername(creatorUsername).orElseThrow(() -> {
            logger.warn("Пользователь не найден: {}", creatorUsername);
            return new UserNotFoundException("Создатель чата не найден");
        });

        // Проверяем, что создатель не добавлен в список участников (он добавляется отдельно)
        if (uniqueUserIds.contains(creator.getId())) {
            logger.warn("Создатель чата {} добавлен в список участников, что недопустимо", creatorUsername);
            throw new IllegalArgumentException("Создатель не должен явно добавляться в список участников");
        }

        Chat chat = new Chat();
        chat.setGroupName(createChatRequestDto.getGroupName());
        chat.setCreatedBy(creator);
        chat.setGroup(true);
        chatRepository.save(chat);
        logger.info("Создан новый чат с ID: {}, названием: {}", chat.getId(), chat.getGroupName());

        ChatMember chatMemberCreator = new ChatMember(chat, creator);
        chatMemberCreator.setIsAdmin(true);
        chatMemberRepository.save(chatMemberCreator);
        logger.info("Создатель чата {} добавлен в участники с правами администратора", creatorUsername);

        for (Long userId : createChatRequestDto.getInitialMemberIds()) {
            User user = userRepository.findById(userId).orElseThrow(() -> {
                logger.warn("Пользователь с ID {} не найден", userId);
                return new UserNotFoundException("Пользователь не найден");
            });
            ChatMember chatMember = new ChatMember(chat, user);
            chatMember.setIsAdmin(false);
            chatMemberRepository.save(chatMember);
            logger.info("Пользователь с ID {} добавлен в участники чата", userId);
        }
        logger.info("Групповой чат успешно создан пользователем: {}", creatorUsername);
    }

    @Transactional
    public void createPrivateChat(CreatePrivateChatRequest createPrivateChatRequest, String creatorUsername) {
        logger.info("Создание приватного чата пользователем: {}, получатель: {}", creatorUsername, createPrivateChatRequest.getRecipientUsername());

        // 1. Получаем информацию об отправителе
        User creator = userRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> {
                    logger.warn("Пользователь не найден: {}", creatorUsername);
                    return new UserNotFoundException("Пользователь с username " + creatorUsername + " не найден");
                });

        // 2. Получаем информацию о получателе
        User recipient = userRepository.findByUsername(createPrivateChatRequest.getRecipientUsername())
                .orElseThrow(() -> {
                    logger.warn("Получатель не найден: {}", createPrivateChatRequest.getRecipientUsername());
                    return new UserNotFoundException("Получатель с username " + createPrivateChatRequest.getRecipientUsername() + " не найден");
                });

        if (creator.getId().equals(recipient.getId())) {
            logger.warn("Попытка создать приватный чат с самим собой для пользователя: {}", creatorUsername);
            throw new IllegalArgumentException("Нельзя создать приватный чат с самим собой");
        }
        // 3. Проверяем, существует ли уже приватный чат между этими пользователями
        Optional<Long> existingChatId = chatMemberRepository.findPrivateChatIdByUserIds(creator.getId(), recipient.getId());
        if (existingChatId.isPresent()) {
            logger.warn("Приватный чат между пользователями {} и {} уже существует", creatorUsername, createPrivateChatRequest.getRecipientUsername());
            throw new IllegalArgumentException("Приватный чат между этими пользователями уже существует");
        }

        // 4. Создаем новый чат
        Chat chat = new Chat();
        chat.setIsGroup(false); // Устанавливаем, что это приватный чат
        chatRepository.save(chat);
        logger.info("Создан новый приватный чат с ID: {}", chat.getId());

        // 5. Создаем участников чата
        ChatMember chatMemberCreator = new ChatMember(chat, creator);
        chatMemberRepository.save(chatMemberCreator);
        logger.info("Пользователь {} добавлен в участники чата", creatorUsername);

        ChatMember chatMemberRecipient = new ChatMember(chat, recipient);
        chatMemberRepository.save(chatMemberRecipient);
        logger.info("Получатель {} добавлен в участники чата", createPrivateChatRequest.getRecipientUsername());

        logger.info("Приватный чат успешно создан пользователем: {}, получатель: {}", creatorUsername, createPrivateChatRequest.getRecipientUsername());
    }

    @Transactional
    public void addChatMember(AddChatMemberRequestDto addChatMemberRequestDto) {
        Long chatId = addChatMemberRequestDto.getChatId();
        Long userId = addChatMemberRequestDto.getUserId();
        logger.info("Добавление участника в чат с ID: {}, ID пользователя: {}", chatId, userId);

        // 1. Проверяем существование чата и пользователя
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> {
                    logger.warn("Чат не найден с ID: {}", chatId);
                    return new UserNotFoundException("Чат не найден");
                });
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("Пользователь не найден с ID: {}", userId);
                    return new UserNotFoundException("Пользователь не найден");
                });

        // 2. Явная проверка на участие в чате
        if (chatMemberRepository.existsByChatIdAndUserId(chatId, userId)) {
            logger.warn("Пользователь с ID {} уже находится в чате с ID {}", userId, chatId);
            throw new IllegalStateException("Пользователь " + userId + " уже находится в чате " + chatId);
        }

        // 3. Создаем новую запись
        ChatMember chatMember = new ChatMember(chat, user);
        chatMember.setIsAdmin(false);
        chatMember.setJoinedAt(OffsetDateTime.now());
        chatMemberRepository.save(chatMember);

        logger.info("Пользователь с ID {} успешно добавлен в чат с ID {}", userId, chatId);
    }

    @Transactional
    public void setAdmin(Long chatId, Long userId, Long requestingUserId) {
        logger.info("Назначение администратора в чате с ID: {}, ID пользователя: {}, инициатор: {}", chatId, userId, requestingUserId);

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> {
                    logger.warn("Чат не найден с ID: {}", chatId);
                    return new UserNotFoundException("Чат не найден");
                });
        // Проверка, что чат групповой
        if (!chat.getGroup()) {
            logger.warn("Попытка назначить администратора в приватном чате с ID: {}", chatId);
            throw new IllegalArgumentException("Приватные чаты не поддерживают администрирование");
        }

        // Проверка прав текущего пользователя
        ChatMember requester = chatMemberRepository.findByChatIdAndUserId(chatId, requestingUserId)
                .orElseThrow(() -> {
                    logger.warn("Запрашивающий пользователь с ID {} не является участником чата с ID {}", requestingUserId, chatId);
                    return new IllegalArgumentException("Вы не участник чата");
                });

        if (!requester.getIsAdmin()) {
            logger.warn("Пользователь с ID {} не является администратором в чате с ID {}", requestingUserId, chatId);
            throw new IllegalArgumentException("Только админ может назначать других админов");
        }

        // Назначение админа
        ChatMember memberToPromote = chatMemberRepository.findByChatIdAndUserId(chatId, userId)
                .orElseThrow(() -> {
                    logger.warn("Участник с ID {} не найден в чате с ID {}", userId, chatId);
                    return new UserNotFoundException("Участник не найден");
                });

        memberToPromote.setIsAdmin(true);
        chatMemberRepository.save(memberToPromote);

        logger.info("Пользователь с ID {} успешно назначен администратором в чате с ID {}", userId, chatId);
    }

    @Transactional
    public void removeChatMember(Long chatId, Long id, Long requestingUserId) {
        logger.info("Удаление участника из чата с ID: {}, ID удаляемого: {}, инициатор: {}", chatId, id, requestingUserId);

        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> {
            logger.warn("Чат не найден с ID: {}", chatId);
            return new IllegalArgumentException("Чат не найден");
        });
        User userToRemove = userRepository.findById(id).orElseThrow(() -> {
            logger.warn("Пользователь для удаления не найден с ID: {}", id);
            return new IllegalArgumentException("User to remove not found");
        });
        User requestingUser = userRepository.findById(requestingUserId).orElseThrow(() -> {
            logger.warn("Запрашивающий пользователь не найден с ID: {}", requestingUserId);
            return new IllegalArgumentException("Requesting user not found");
        });

        if (!chat.getGroup()) {
            logger.warn("Попытка удаления участника из приватного чата с ID: {}", chatId);
            throw new IllegalArgumentException("Приватные чаты не поддерживают администрирование");
        }

        // Проверяем, является ли запрашивающий пользователь админом чата
        ChatMemberId requestingUserChatMemberId = new ChatMemberId(chatId, requestingUserId);
        ChatMember requestingUserChatMember = chatMemberRepository.findById(requestingUserChatMemberId)
                .orElseThrow(() -> {
                    logger.warn("Запрашивающий пользователь с ID {} не является участником чата с ID {}", requestingUserId, chatId);
                    return new IllegalArgumentException("Запрашивающий пользователь не является участником чата");
                });

        if (!requestingUserChatMember.getIsAdmin()) {
            logger.warn("Пользователь с ID {} не является администратором в чате с ID {}", requestingUserId, chatId);
            throw new IllegalArgumentException("Только админ может удалять других пользователей");
        }

        // Проверяем, существует ли участник чата, которого нужно удалить
        ChatMemberId chatMemberIdToRemove = new ChatMemberId(chatId, id);
        if (!chatMemberRepository.existsById(chatMemberIdToRemove)) {
            logger.warn("Участник чата для удаления не найден с ID: {}", id);
            throw new UserNotFoundException("Участник чата не найден");
        }

        chatMemberRepository.deleteById(chatMemberIdToRemove);

        logger.info("Пользователь с ID {} успешно удален из чата с ID {}", id, chatId);
    }
}