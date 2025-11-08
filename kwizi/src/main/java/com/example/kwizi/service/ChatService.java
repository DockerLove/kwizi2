package com.example.kwizi.service;

import com.example.kwizi.DTO.request.AddChatMemberRequestDto;
import com.example.kwizi.DTO.request.CreateGroupChatRequest;
import com.example.kwizi.DTO.request.CreatePrivateChatRequest;
import com.example.kwizi.enums.ChatRole;
import com.example.kwizi.exception.ChatNotFoundException;
import com.example.kwizi.exception.ChatService.*;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.model.Chat;
import com.example.kwizi.model.ChatMember;
import com.example.kwizi.model.ChatMember.ChatMemberId;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.ChatMemberRepository;
import com.example.kwizi.repository.ChatRepository;
import com.example.kwizi.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final UserRepository userRepository;
    private final SystemMessageService systemMessageService;
    private final NotificationService notificationService;

    @Autowired
    public ChatService(ChatRepository chatRepository, ChatMemberRepository chatMemberRepository,
                       UserRepository userRepository,SystemMessageService systemMessageService,
                       NotificationService notificationService) {
        this.chatRepository = chatRepository;
        this.chatMemberRepository = chatMemberRepository;
        this.userRepository = userRepository;
        this.systemMessageService = systemMessageService;
        this.notificationService = notificationService;
    }

    public void createGroupChat(CreateGroupChatRequest createChatRequestDto, String creatorUsername) {
        logger.info("Создание группового чата пользователем: {}", creatorUsername);

        User creator = findUserByUsername(creatorUsername);
        validateGroupChatRequest(createChatRequestDto, creator);

        Chat chat = createAndSaveChat(createChatRequestDto, creator);
        addCreatorAsAdmin(chat, creator);
        addInitialMembers(chat, createChatRequestDto.getInitialMemberIds());

        logger.info("Групповой чат успешно создан пользователем: {}", creatorUsername);
    }

    public void createPrivateChat(CreatePrivateChatRequest createPrivateChatRequest, String creatorUsername) {
        logger.info("Создание приватного чата пользователем: {}, получатель: {}", creatorUsername, createPrivateChatRequest.getRecipientUsername());

        User creator = findUserByUsername(creatorUsername);
        User recipient = findUserByUsername(createPrivateChatRequest.getRecipientUsername());

        validatePrivateChatCreation(creator, recipient);

        Chat chat = createPrivateChat();
        addChatMember(chat, creator);
        addChatMember(chat, recipient);

        logger.info("Приватный чат успешно создан между {} и {}", creatorUsername, createPrivateChatRequest.getRecipientUsername());
    }

    public void addChatMember(AddChatMemberRequestDto addChatMemberRequestDto,String addedByUsername) {
        Long chatId = addChatMemberRequestDto.getChatId();
        Long userId = addChatMemberRequestDto.getUserId();
        logger.info("Добавление участника в чат с ID: {}, ID пользователя: {}", chatId, userId);

        Chat chat = findChatById(chatId);
        User user = findUserById(userId);

        validateChatMemberAdditionAndChatIsGroup(chat, userId);

        addChatMember(chat, user);

        systemMessageService.createUserAddedMessage(chat, user.getUsername(), addedByUsername);

        notificationService.notifyUserAdded(chatId, user.getUsername(), addedByUsername);
        logger.info("Пользователь с ID {} успешно добавлен в чат с ID {}", userId, chatId);
    }

    public void setAdmin(Long chatId, Long userId, Long requestingUserId) {
        logger.info("Назначение администратора в чате с ID: {}, ID пользователя: {}, инициатор: {}", chatId, userId, requestingUserId);

        Chat chat = findChatById(chatId);
        User requestingUser = findUserById(requestingUserId);
        User targetUser = findUserById(userId);

        validatePromotionRights(chat,chatId, requestingUserId, userId);

        ChatMember memberToPromote = findChatMember(chatId, userId);

        if (memberToPromote.isAdmin()) {
            throw new BusinessLogicException("Пользователь уже является администратором");
        }

        promoteToAdmin(memberToPromote);

        systemMessageService.createUserPromotedMessage(
                chat,
                targetUser.getUsername(),
                requestingUser.getUsername()
        );

        notificationService.notifyUserPromoted(
                chatId,
                targetUser.getUsername(),
                requestingUser.getUsername()
        );
        logger.info("Пользователь с ID {} успешно назначен администратором в чате с ID {}", userId, chatId);
    }

    private void validatePromotionRights(Chat chat,Long chatId, Long requestingUserId, Long targetUserId) {
        ChatMember requester = findChatMember(chatId, requestingUserId);
        if (!chat.getIsGroup()) {
            throw new ChatOperationNotAllowedException("Операция не поддерживается для приватных чатов");
        }

        if ((!requester.isAdmin() || requester.isAdmin()) && !requester.isOwner()) {
            throw new AccessDeniedException("Только владелец может назначать администраторов");
        }

        if (requester.isOwner() && requestingUserId.equals(targetUserId)) {
            throw new BusinessLogicException("Владелец не может назначить себя администратором");
        }
    }


    public void removeChatMember(Long chatId, Long userIdToRemove, Long requestingUserId) {
        logger.info("Удаление участника из чата с ID: {}, ID удаляемого: {}, инициатор: {}", chatId, userIdToRemove, requestingUserId);

        Chat chat = findChatById(chatId);
        User requestingUser = findUserById(requestingUserId);
        User removedUser = findUserById(userIdToRemove);

        if (!chat.getIsGroup()) {
            throw new ChatOperationNotAllowedException("Операция не поддерживается для приватных чатов");
        }

        validateRemovalRights(chatId, requestingUserId, userIdToRemove);

        ChatMember memberToRemove = findChatMember(chatId, userIdToRemove);

        // Удаляем участника
        removeMemberFromChat(memberToRemove);

        systemMessageService.createUserRemovedMessage(
                chat,
                removedUser.getUsername(),
                requestingUser.getUsername()
        );

        notificationService.notifyUserRemoved(
                chatId,
                removedUser.getUsername(),
                requestingUser.getUsername()
        );
        logger.info("Пользователь с ID {} успешно удален из чата с ID {}", userIdToRemove, chatId);
    }

    private void removeMemberFromChat(ChatMember member) {
        chatMemberRepository.delete(member);

        logger.debug("Участник с ID {} удален из чата с ID {}",
                member.getUser().getId(), member.getChat().getId());
    }

    private void validateRemovalRights(Long chatId, Long requestingUserId, Long targetUserId) {
        ChatMember requester = findChatMember(chatId, requestingUserId);
        ChatMember target = findChatMember(chatId, targetUserId);

        // Владелец может удалить кого угодно (кроме себя)
        if (requester.isOwner()) {
            if (requester.getUser().getId().equals(target.getUser().getId())) {
                throw new BusinessLogicException("Владелец не может удалить сам себя");
            }
            return; // Владелец может удалить любого
        }

        // Админ может удалить только обычных участников
        if (requester.isAdmin()) {
            if (target.isOwner() || target.isAdmin()) {
                throw new AccessDeniedException("Администратор не может удалить владельца или другого администратора");
            }
            // Админ не может удалить себя
            if (requester.getUser().getId().equals(target.getUser().getId())) {
                throw new BusinessLogicException("Администратор не может удалить сам себя");
            }
            return;
        }

        // Обычный участник не может никого удалять
        throw new AccessDeniedException("Только владелец и администраторы могут удалять участников");
    }

    public void leaveChat(Long chatId, Long userId) {
        logger.info("Начало процедуры выхода пользователя из чата. ChatID: {}, UserID: {}", chatId, userId);

        Chat chat = findChatById(chatId);
        User user = findUserById(userId);


        logger.debug("Чат найден. ChatID: {}, Название: {}", chatId, chat.getGroup());

        if(!chat.getGroup()){
            throw new NotGroupChatException("Вы не можете покинуть приватный чат");
        }

        ChatMember member = chatMemberRepository.findByChatIdAndUserId(chatId, userId)
                .orElseThrow(() -> {
                    throw new ChatMemberNotFoundException("Пользователь не является участником чата");
                });

        if (chat.getCreatedBy().getId().equals(userId)) {
            throw new BusinessLogicException("Создатель чата не может его покинуть. Передайте права или удалите чат");
        }

        // Удаляем пользователя из чата
        chatMemberRepository.delete(member);
        logger.info("Пользователь удален из членов чата. ChatID: {}, UserID: {}", chatId, userId);


        chatRepository.save(chat);

        systemMessageService.createUserLeftMessage(
                chat,
                user.getUsername()  // Кто вышел
        );

        // 2. Отправляем WebSocket уведомление
        notificationService.notifyUserLeft(
                chatId,
                user.getUsername()  // Кто вышел
        );

        // Логируем успешное завершение операции
        logger.info("Пользователь успешно покинул чат. ChatID: {}, UserID: {}", chatId, userId);
        logger.info("Системное сообщение и уведомление отправлены для выхода пользователя");
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден: " + username));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));
    }

    private Chat findChatById(Long chatId) {
        return chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Чат не найден"));
    }

    private ChatMember findChatMember(Long chatId, Long userId) {
        return chatMemberRepository.findByChatIdAndUserId(chatId, userId)
                .orElseThrow(() -> new ChatMemberNotFoundException("Пользователь не является участником чата"));
    }

    private void validateGroupChatRequest(CreateGroupChatRequest request, User creator) {
        Set<Long> uniqueUserIds = new HashSet<>(request.getInitialMemberIds());

        if (uniqueUserIds.size() != request.getInitialMemberIds().size()) {
            throw new DuplicateChatMemberException("Список участников содержит дубликаты");
        }

        // Автоматически исключаем создателя из списка участников
        uniqueUserIds.remove(creator.getId());
        request.getInitialMemberIds().removeIf(id -> id.equals(creator.getId()));
    }

    private void validatePrivateChatCreation(User creator, User recipient) {
        if (creator.getId().equals(recipient.getId())) {
            throw new ChatOperationNotAllowedException("Нельзя создать приватный чат с самим собой");
        }

        if (chatMemberRepository.findPrivateChatIdByUserIds(creator.getId(), recipient.getId()).isPresent()) {
            throw new DuplicateChatMemberException("Приватный чат между этими пользователями уже существует");
        }
    }


    private void validateChatMemberAdditionAndChatIsGroup(Chat chat, Long userId) {
        if (chat.getGroupName() == null) {
            throw new ChatOperationNotAllowedException("Добавлять участников можно только в групповые чаты");
        }
        if (chatMemberRepository.existsByChatIdAndUserId(chat.getId(), userId)) {
            throw new DuplicateChatMemberException("Пользователь уже находится в чате");
        }
    }


    private void validateAdminRightsAndGroupChat(Chat chat,Long chatId, Long userId) {
        ChatMember requester = findChatMember(chatId, userId);
        if (!chat.getIsGroup()) {
            throw new ChatOperationNotAllowedException("Операция не поддерживается для приватных чатов");
        }
        if (requester.getRole().equals(ChatRole.MEMBER)) {
            throw new InsufficientPermissionsException("Только владелец или администратор могут удалять пользователей");
        }
    }

    private void validateChatMemberExists(Long chatId, Long userId) {
        if (!chatMemberRepository.existsByChatIdAndUserId(chatId, userId)) {
            throw new ChatMemberNotFoundException("Участник чата не найден");
        }
    }

    private Chat createAndSaveChat(CreateGroupChatRequest request, User creator) {
        Chat chat = new Chat();
        chat.setGroupName(request.getGroupName());
        chat.setCreatedBy(creator);
        chat.setIsGroup(true);
        return chatRepository.save(chat);
    }

    private Chat createPrivateChat() {
        Chat chat = new Chat();
        chat.setIsGroup(false);
        return chatRepository.save(chat);
    }

    private void addCreatorAsAdmin(Chat chat, User creator) {
        ChatMember chatMemberCreator = new ChatMember(chat, creator);
        chatMemberCreator.setRole(ChatRole.OWNER);
        chatMemberRepository.save(chatMemberCreator);
    }

    private void addInitialMembers(Chat chat, List<Long> userIds) {
        if (userIds.isEmpty()) return;

        List<User> users = userRepository.findAllById(userIds);
        if (users.size() != userIds.size()) {
            throw new UserNotFoundException("Один или несколько пользователей не найдены");
        }

        List<ChatMember> chatMembers = users.stream()
                .map(user -> {
                    ChatMember member = new ChatMember(chat, user);
                    member.setRole(ChatRole.MEMBER);
                    return member;
                })
                .collect(Collectors.toList());

        chatMemberRepository.saveAll(chatMembers);
    }

    private void addChatMember(Chat chat, User user) {
        ChatMember chatMember = new ChatMember(chat, user);
        chatMember.setRole(ChatRole.MEMBER);
        chatMember.setJoinedAt(OffsetDateTime.now());
        chatMemberRepository.save(chatMember);
    }

    private void promoteToAdmin(ChatMember member) {
        member.setRole(ChatRole.ADMIN);
    }

    private void removeChatMember(Long chatId, Long userId) {
        ChatMemberId chatMemberId = new ChatMemberId(chatId, userId);
        chatMemberRepository.deleteById(chatMemberId);
    }
}