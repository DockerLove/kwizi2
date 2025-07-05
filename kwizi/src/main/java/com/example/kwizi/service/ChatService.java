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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

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

        Set<Long> uniqueUserIds = new HashSet<>(createChatRequestDto.getInitialMemberIds());
        if (uniqueUserIds.size() < createChatRequestDto.getInitialMemberIds().size()) {
            throw new IllegalArgumentException("Список участников содержит дубликаты");
        }
        User creator = userRepository.findByUsername(creatorUsername).orElseThrow(() -> new UserNotFoundException("Создатель чата не найден"));

        // Проверяем, что создатель не добавлен в список участников (он добавляется отдельно)
        if (uniqueUserIds.contains(creator.getId())) {
            throw new IllegalArgumentException("Создатель не должен явно добавляться в список участников");
        }
        Chat chat = new Chat();
        chat.setGroupName(createChatRequestDto.getGroupName());

        chat.setCreatedBy(creator);
        chat.setGroup(true);
        chatRepository.save(chat);
        ChatMember chatMemberCreator = new ChatMember(chat, creator);
        chatMemberCreator.setIsAdmin(true);
        chatMemberRepository.save(chatMemberCreator);

        for (Long userId : createChatRequestDto.getInitialMemberIds()) {
            User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));
            ChatMember chatMember = new ChatMember(chat, user);
            chatMember.setIsAdmin(false);
            chatMemberRepository.save(chatMember);
        }
    }

    @Transactional
    public void createPrivateChat(CreatePrivateChatRequest createPrivateChatRequest, String creatorUsername) {
        // 1. Получаем информацию об отправителе
        User creator = userRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с username " + creatorUsername + " не найден"));

        // 2. Получаем информацию о получателе
        User recipient = userRepository.findByUsername(createPrivateChatRequest.getRecipientUsername())
                .orElseThrow(() -> new UserNotFoundException("Получатель с username " + createPrivateChatRequest.getRecipientUsername() + " не найден"));

        if (creator.getId().equals(recipient.getId())) {
            throw new IllegalArgumentException("Нельзя создать приватный чат с самим собой");
        }
        // 3. Проверяем, существует ли уже приватный чат между этими пользователями
        Optional<Long> existingChatId = chatMemberRepository.findPrivateChatIdByUserIds(creator.getId(), recipient.getId());
        if (existingChatId.isPresent()) {
            throw new IllegalArgumentException("Приватный чат между этими пользователями уже существует");
        }

        // 4. Создаем новый чат
        Chat chat = new Chat();
        chat.setIsGroup(false); // Устанавливаем, что это приватный чат
        chatRepository.save(chat);

        // 5. Создаем участников чата
        ChatMember chatMemberCreator = new ChatMember(chat, creator);
        chatMemberRepository.save(chatMemberCreator);

        ChatMember chatMemberRecipient = new ChatMember(chat, recipient);
        chatMemberRepository.save(chatMemberRecipient);
    }

    @Transactional
    public void addChatMember(AddChatMemberRequestDto addChatMemberRequestDto) {
        Long chatId = addChatMemberRequestDto.getChatId();
        Long userId = addChatMemberRequestDto.getUserId();

        // 1. Проверяем существование чата и пользователя
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new UserNotFoundException("Чат не найден"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        // 2. Явная проверка на участие в чате
        if (chatMemberRepository.existsByChatIdAndUserId(chatId, userId)) {
            throw new IllegalStateException("Пользователь " + userId + " уже находится в чате " + chatId);
        }

        // 3. Создаем новую запись
        ChatMember chatMember = new ChatMember(chat, user);
        chatMember.setIsAdmin(false);
        chatMember.setJoinedAt(OffsetDateTime.now());
        chatMemberRepository.save(chatMember);
    }

    @Transactional
    public void setAdmin(Long chatId, Long userId, Long requestingUserId) {

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new UserNotFoundException("Чат не найден"));
        // Проверка, что чат групповой
        if (!chat.getGroup()) {
            throw new IllegalArgumentException("Приватные чаты не поддерживают администрирование");
        }

        // Проверка прав текущего пользователя
        ChatMember requester = chatMemberRepository.findByChatIdAndUserId(chatId, requestingUserId)
                .orElseThrow(() -> new IllegalArgumentException("Вы не участник чата"));

        if (!requester.getIsAdmin()) {
            throw new IllegalArgumentException("Только админ может назначать других админов");
        }

        // Назначение админа
        ChatMember memberToPromote = chatMemberRepository.findByChatIdAndUserId(chatId, userId)
                .orElseThrow(() -> new UserNotFoundException("Участник не найден"));

        memberToPromote.setIsAdmin(true);
        chatMemberRepository.save(memberToPromote);
    }

    @Transactional
    public void removeChatMember(Long chatId, Long id, Long requestingUserId) {
        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new IllegalArgumentException("Чат не найден"));
        User userToRemove = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User to remove not found"));
        User requestingUser = userRepository.findById(requestingUserId).orElseThrow(() -> new IllegalArgumentException("Requesting user not found"));

        if (!chat.getGroup()) {
            throw new IllegalArgumentException("Приватные чаты не поддерживают администрирование");
        }

        // Проверяем, является ли запрашивающий пользователь админом чата
        ChatMemberId requestingUserChatMemberId = new ChatMemberId(chatId, requestingUserId);
        ChatMember requestingUserChatMember = chatMemberRepository.findById(requestingUserChatMemberId)
                .orElseThrow(() -> new IllegalArgumentException("Запрашивающий пользователь не является участником чата"));

        if (!requestingUserChatMember.getIsAdmin()) {
            throw new IllegalArgumentException("Только админ может удалять других пользователей");
        }

        // Проверяем, существует ли участник чата, которого нужно удалить
        ChatMemberId chatMemberIdToRemove = new ChatMemberId(chatId, id);
        if (!chatMemberRepository.existsById(chatMemberIdToRemove)) {
            throw new UserNotFoundException("Участник чата не найден");
        }

        chatMemberRepository.deleteById(chatMemberIdToRemove);
    }
}