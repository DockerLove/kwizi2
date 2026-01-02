package com.example.kwizi.service;

import com.example.kwizi.DTO.request.AddChatMemberRequestDto;
import com.example.kwizi.DTO.request.CreateGroupChatRequest;
import com.example.kwizi.DTO.request.CreatePrivateChatRequest;
import com.example.kwizi.DTO.response.ChatPreviewResponse;
import com.example.kwizi.enums.ChatRole;
import com.example.kwizi.enums.ChatType;
import com.example.kwizi.exception.BusinessLogicException;
import com.example.kwizi.exception.ChatNotFoundException;
import com.example.kwizi.exception.ChatService.*;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.model.Chat;
import com.example.kwizi.model.ChatMember;
import com.example.kwizi.model.GroupChat;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.ChatMemberRepository;
import com.example.kwizi.repository.ChatRepository;
import com.example.kwizi.repository.MessageRepository;
import com.example.kwizi.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
    private final FileStorageService fileStorageService;
    private final MessageRepository messageRepository;

    @Autowired
    public ChatService(ChatRepository chatRepository, ChatMemberRepository chatMemberRepository,
                       UserRepository userRepository, SystemMessageService systemMessageService,
                       NotificationService notificationService, FileStorageService fileStorageService,
                       MessageRepository messageRepository) {
        this.chatRepository = chatRepository;
        this.chatMemberRepository = chatMemberRepository;
        this.userRepository = userRepository;
        this.systemMessageService = systemMessageService;
        this.notificationService = notificationService;
        this.fileStorageService = fileStorageService;
        this.messageRepository = messageRepository;
    }

    public void createGroupChat(CreateGroupChatRequest createChatRequestDto, String creatorUsername) {
        logger.info("Создание группового чата пользователем: {}", creatorUsername);

        User creator = findUserByUsername(creatorUsername);
        validateGroupChatRequest(createChatRequestDto, creator);

        Chat chat = createAndSaveChat(createChatRequestDto, creator);
        addCreatorAsAdmin(chat, creator);
        addInitialMembers(chat, createChatRequestDto.getInitialMemberIds());
        updateChatActivity(chat.getId());

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

        updateChatActivity(chat.getId());

        logger.info("Приватный чат успешно создан между {} и {}", creatorUsername, createPrivateChatRequest.getRecipientUsername());
    }

    public void addChatMember(AddChatMemberRequestDto addChatMemberRequestDto, String addedByUsername) {
        Long chatId = addChatMemberRequestDto.getChatId();
        Long userId = addChatMemberRequestDto.getUserId();
        logger.info("Добавление участника в чат с ID: {}, ID пользователя: {}", chatId, userId);

        Chat chat = findChatById(chatId);
        User user = findUserById(userId);

        validateChatMemberAdditionAndChatIsGroup(chat, userId);

        addChatMember(chat, user);

        updateChatActivity(chatId);

        systemMessageService.createUserAddedMessage(chat, user.getUsername(), addedByUsername);

        notificationService.notifyUserAdded(chatId, user.getUsername(), addedByUsername);
        logger.info("Пользователь с ID {} успешно добавлен в чат с ID {}", userId, chatId);
    }

    public void setAdmin(Long chatId, Long userId, Long requestingUserId) {
        logger.info("Назначение администратора в чате с ID: {}, ID пользователя: {}, инициатор: {}", chatId, userId, requestingUserId);

        Chat chat = findChatById(chatId);
        User requestingUser = findUserById(requestingUserId);
        User targetUser = findUserById(userId);

        validatePromotionRights(chat, chatId, requestingUserId, userId);

        ChatMember memberToPromote = findChatMember(chatId, userId);

        if (memberToPromote.isAdmin()) {
            throw new BusinessLogicException("Пользователь уже является администратором");
        }

        promoteToAdmin(memberToPromote);

        updateChatActivity(chatId);

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

    public void demoteAdminToMember(Long chatId, Long targetUserId, Long requestingUserId) {
        logger.info("Разжалование администратора в чате с ID: {}, ID пользователя: {}, инициатор: {}",
                chatId, targetUserId, requestingUserId);

        Chat chat = findChatById(chatId);
        User requestingUser = findUserById(requestingUserId);
        User targetUser = findUserById(targetUserId);

        if (chat.getChatType() == ChatType.PRIVATE) {
            throw new ChatOperationNotAllowedException("Операция не поддерживается для приватных чатов");
        }

        validateDemotionRights(chatId, requestingUserId, targetUserId);

        ChatMember memberToDemote = findChatMember(chatId, targetUserId);

        demoteToMember(memberToDemote);

        updateChatActivity(chatId);

        systemMessageService.createUserDemotedMessage(
                chat,
                targetUser.getUsername(),
                requestingUser.getUsername()
        );

        notificationService.notifyUserDemoted(
                chatId,
                targetUser.getUsername(),
                requestingUser.getUsername()
        );

        logger.info("Пользователь с ID {} разжалован из администраторов в чате с ID {}", targetUserId, chatId);
    }

    public void removeChatMember(Long chatId, Long userIdToRemove, Long requestingUserId) {
        logger.info("Удаление участника из чата с ID: {}, ID удаляемого: {}, инициатор: {}", chatId, userIdToRemove, requestingUserId);

        Chat chat = findChatById(chatId);
        User requestingUser = findUserById(requestingUserId);
        User removedUser = findUserById(userIdToRemove);

        if ((chat.getChatType() == ChatType.PRIVATE)) {
            throw new ChatOperationNotAllowedException("Операция не поддерживается для приватных чатов");
        }

        validateRemovalRights(chatId, requestingUserId, userIdToRemove);

        ChatMember memberToRemove = findChatMember(chatId, userIdToRemove);

        removeMemberFromChat(memberToRemove);

        updateChatActivity(chatId);

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

    public void leaveChat(Long chatId, Long userId) {
        logger.info("Начало процедуры выхода пользователя из чата. ChatID: {}, UserID: {}", chatId, userId);

        Chat chat = findChatById(chatId);
        User user = findUserById(userId);

        if (chat.getChatType() == ChatType.PRIVATE) {
            throw new NotGroupChatException("Вы не можете покинуть приватный чат");
        }

        ChatMember member = chatMemberRepository.findByChatIdAndUserId(chatId, userId)
                .orElseThrow(() -> {
                    throw new ChatMemberNotFoundException("Пользователь не является участником чата");
                });

        if (chat.getCreatedBy().getId().equals(userId)) {
            throw new BusinessLogicException("Создатель чата не может его покинуть. Передайте права или удалите чат");
        }

        chatMemberRepository.delete(member);
        logger.info("Пользователь удален из членов чата. ChatID: {}, UserID: {}", chatId, userId);

        updateChatActivity(chatId);

        chatRepository.save(chat);

        systemMessageService.createUserLeftMessage(
                chat,
                user.getUsername()
        );

        notificationService.notifyUserLeft(
                chatId,
                user.getUsername()
        );

        logger.info("Пользователь успешно покинул чат. ChatID: {}, UserID: {}", chatId, userId);
        logger.info("Системное сообщение и уведомление отправлены для выхода пользователя");
    }

    public void updateGroupName(Long chatId, String newGroupName, Long requesterId) {
        logger.info("Изменение названия группы chatId: {}, новое название: {}, инициатор: {}",
                chatId, newGroupName, requesterId);

        Chat chat = findChatById(chatId);

        if (chat.getChatType() == ChatType.PRIVATE) {
            throw new BusinessLogicException("Операция доступна только для групповых чатов");
        }

        ChatMember requester = findChatMember(chatId, requesterId);
        if (requester.getRole() != ChatRole.OWNER && requester.getRole() != ChatRole.ADMIN) {
            throw new AccessDeniedException("Только владелец и администраторы могут изменять название группы");
        }

        GroupChat groupChat = chat.getGroupChat();
        String oldGroupName = groupChat.getGroupName();
        groupChat.setGroupName(newGroupName);

        chatRepository.save(chat);

        updateChatActivity(chatId);

        systemMessageService.createGroupNameChangedMessage(
                chat,
                oldGroupName,
                newGroupName,
                requester.getUser().getUsername()
        );

        notificationService.notifyGroupNameChanged(
                chatId,
                oldGroupName,
                newGroupName,
                requester.getUser().getUsername()
        );

        logger.info("Название группы изменено с '{}' на '{}'", oldGroupName, newGroupName);
    }

    public void updateChatAvatar(Long chatId, MultipartFile file, Long requesterId) {
        logger.info("Обновление аватара для чата с ID: {}", chatId);

        Chat chat = findChatById(chatId);
        ChatMember requester = findChatMember(chatId, requesterId);

        if (chat.getChatType() == ChatType.PRIVATE) {
            throw new BusinessLogicException("Аватар можно установить только для групповых чатов");
        }
        if (file.isEmpty()) {
            throw new BusinessLogicException("Файл не может быть пустым");
        }

        String avatarUrl = fileStorageService.saveChatAvatar(file, chatId);

        GroupChat groupChat = chat.getGroupChat();
        groupChat.setAvatarUrl(avatarUrl);
        chatRepository.save(chat);

        updateChatActivity(chatId);

        systemMessageService.createGroupPhotoChangedMessage(
                chat,
                requester.getUser().getUsername()
        );

        notificationService.notifyGroupPhotoChanged(
                chatId,
                requester.getUser().getUsername()
        );
        logger.info("Аватар успешно обновлен для чата с ID: {}", chatId);
    }

    public void updateChatActivity(Long chatId) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatNotFoundException("Чат не найден"));

        chat.setLastActivityAt(OffsetDateTime.now());
        chatRepository.save(chat);
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

    private Chat createAndSaveChat(CreateGroupChatRequest request, User creator) {
        Chat chat = new Chat();
        chat.setChatType(ChatType.GROUP);

        GroupChat groupChat = new GroupChat(chat, request.getGroupName());
        chat.setGroupChat(groupChat);

        chat = chatRepository.save(chat);

        ChatMember creatorMember = new ChatMember(chat, creator, ChatRole.OWNER);
        chatMemberRepository.save(creatorMember);

        return chat;
    }

    private Chat createPrivateChat() {
        Chat chat = new Chat();
        chat.setChatType(ChatType.PRIVATE);
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

    private void validatePromotionRights(Chat chat, Long chatId, Long requestingUserId, Long targetUserId) {
        ChatMember requester = findChatMember(chatId, requestingUserId);
        if (chat.getChatType() == ChatType.PRIVATE) {
            throw new ChatOperationNotAllowedException("Операция не поддерживается для приватных чатов");
        }

        if ((!requester.isAdmin() || requester.isAdmin()) && !requester.isOwner()) {
            throw new AccessDeniedException("Только владелец может назначать администраторов");
        }

        if (requester.isOwner() && requestingUserId.equals(targetUserId)) {
            throw new BusinessLogicException("Владелец не может назначить себя администратором");
        }
    }

    private void removeMemberFromChat(ChatMember member) {
        chatMemberRepository.delete(member);

        logger.debug("Участник с ID {} удален из чата с ID {}",
                member.getUser().getId(), member.getChat().getId());
    }

    private void validateRemovalRights(Long chatId, Long requestingUserId, Long targetUserId) {
        ChatMember requester = findChatMember(chatId, requestingUserId);
        ChatMember target = findChatMember(chatId, targetUserId);

        if (requester.isOwner()) {
            if (requester.getUser().getId().equals(target.getUser().getId())) {
                throw new BusinessLogicException("Владелец не может удалить сам себя");
            }
            return;
        }

        if (requester.isAdmin()) {
            if (target.isOwner() || target.isAdmin()) {
                throw new AccessDeniedException("Администратор не может удалить владельца или другого администратора");
            }
            if (requester.getUser().getId().equals(target.getUser().getId())) {
                throw new BusinessLogicException("Администратор не может удалить сам себя");
            }
            return;
        }

        throw new AccessDeniedException("Только владелец и администраторы могут удалять участников");
    }

    private void validateDemotionRights(Long chatId, Long requestingUserId, Long targetUserId) {
        ChatMember requester = findChatMember(chatId, requestingUserId);
        ChatMember target = findChatMember(chatId, targetUserId);

        if (!requester.isOwner()) {
            throw new AccessDeniedException("Только владелец может разжаловать администратора");
        }

        if (target.isOwner()) {
            throw new BusinessLogicException("Нельзя разжаловать владельца чата");
        }

        if (requestingUserId.equals(targetUserId)) {
            throw new BusinessLogicException("Владелец не может разжаловать сам себя");
        }

        if (!target.isAdmin() || target.isOwner()) {
            throw new BusinessLogicException("Пользователь не является администратором");
        }
    }

    private void demoteToMember(ChatMember member) {
        member.setRole(ChatRole.MEMBER);
        chatMemberRepository.save(member);
    }

    public Page<ChatPreviewResponse> getUserChatsPreview(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "lastActivityAt"));

        Page<Chat> chatPage = chatRepository.findUserChatsOrderByLastActivity(userId, pageable);

        return chatPage.map(chat -> mapToChatPreviewDto(chat, userId));
    }

    private ChatPreviewResponse mapToChatPreviewDto(Chat chat, Long currentUserId) {
        ChatPreviewResponse dto = new ChatPreviewResponse();
        dto.setId(chat.getId());
        dto.setChatType(chat.getChatType());
        dto.setLastActivityAt(chat.getLastActivityAt());
        dto.setLastMessagePreview(getLastMessagePreview(chat.getId()));

        dto.setDisplayName(getChatDisplayName(chat, currentUserId));

        return dto;
    }

    private String getChatDisplayName(Chat chat, Long currentUserId) {
        if (chat.getChatType() == ChatType.GROUP && chat.getGroupChat() != null) {
            return chat.getGroupChat().getGroupName();
        } else if (chat.getChatType() == ChatType.PRIVATE) {
            return getChatPartnerUsername(chat, currentUserId);
        }

        return "Неизвестный чат";
    }

    private String getChatPartnerUsername(Chat chat, Long currentUserId) {
        return chat.getChatMembers().stream()
                .filter(member -> !member.getUser().getId().equals(currentUserId))
                .findFirst()
                .map(member -> member.getUser().getUsername())
                .orElse("Неизвестный пользователь");
    }

    private String getLastMessagePreview(Long chatId) {
        return messageRepository.findLastMessagePreviewByChatId(chatId)
                .orElse("Чат создан");
    }
}