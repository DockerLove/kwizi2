package com.example.kwizi.service;

import com.example.kwizi.DTO.request.AddChatMemberRequestDto;
import com.example.kwizi.DTO.request.CreateGroupChatRequest;
import com.example.kwizi.DTO.request.CreatePrivateChatRequest;
import com.example.kwizi.model.Chat;
import com.example.kwizi.model.ChatMember;
import com.example.kwizi.model.ChatMember.ChatMemberId;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.ChatMemberRepository;
import com.example.kwizi.repository.ChatRepository;
import com.example.kwizi.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.Optional;

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
    public void createGroupChat(CreateGroupChatRequest createChatRequestDto) {
        Chat chat = new Chat();
        chat.setGroupName(createChatRequestDto.getGroupName());

        User creator = userRepository.findById(createChatRequestDto.getCreatorId()).orElseThrow(() -> new IllegalArgumentException("Creator not found"));
        chat.setCreatedBy(creator);
        chat.setGroup(true);
        chatRepository.save(chat);
        ChatMember chatMemberCreator = new ChatMember(chat, creator);
        chatMemberCreator.setIsAdmin(true);
        chatMemberRepository.save(chatMemberCreator);

        for (Long userId : createChatRequestDto.getInitialMemberIds()) {
            User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
            ChatMember chatMember = new ChatMember(chat, user);
            chatMember.setIsAdmin(false);
            chatMemberRepository.save(chatMember);
        }
    }

    @Transactional
    public void createPrivateChat(CreatePrivateChatRequest createPrivateChatRequest, String creatorUsername) {
        // 1. Получаем информацию об отправителе
        User creator = userRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь с username " + creatorUsername + " не найден"));

        // 2. Получаем информацию о получателе
        User recipient = userRepository.findByUsername(createPrivateChatRequest.getRecipientUsername())
                .orElseThrow(() -> new IllegalArgumentException("Получатель с username " + createPrivateChatRequest.getRecipientUsername() + " не найден"));

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
                .orElseThrow(() -> new IllegalArgumentException("Chat not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 2. Явная проверка на участие в чате
        if (chatMemberRepository.existsByChatIdAndUserId(chatId, userId)) {
            throw new IllegalStateException("User " + userId + " is already a member of chat " + chatId);
        }

        // 3. Создаем новую запись
        ChatMember chatMember = new ChatMember(chat, user);
        chatMember.setIsAdmin(false);
        chatMember.setJoinedAt(OffsetDateTime.now());
        chatMemberRepository.save(chatMember);
    }

    @Transactional
    public void setAdmin(Long chatId, Long userId) {
        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new IllegalArgumentException("Chat not found"));
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (chat.getGroup()) { // Допустим, в Chat есть поле isPrivate
            throw new IllegalArgumentException("You cannot remove members from a private chat.");
        }
        ChatMemberId chatMemberId = new ChatMemberId(chatId, userId);
        ChatMember chatMember = chatMemberRepository.findById(chatMemberId).orElseThrow(() -> new IllegalArgumentException("Chat member not found"));
        chatMember.setIsAdmin(true);
        chatMemberRepository.save(chatMember);
    }

    @Transactional
    public void removeChatMember(Long chatId, Long id, Long requestingUserId) {
        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new IllegalArgumentException("Chat not found"));
        User userToRemove = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User to remove not found"));
        User requestingUser = userRepository.findById(requestingUserId).orElseThrow(() -> new IllegalArgumentException("Requesting user not found"));

        if (chat.getGroup()) { // Допустим, в Chat есть поле isPrivate
            throw new IllegalArgumentException("You cannot remove members from a private chat.");
        }

        // Проверяем, является ли запрашивающий пользователь админом чата
        ChatMemberId requestingUserChatMemberId = new ChatMemberId(chatId, requestingUserId);
        ChatMember requestingUserChatMember = chatMemberRepository.findById(requestingUserChatMemberId)
                .orElseThrow(() -> new IllegalArgumentException("Requesting user is not a member of the chat"));

        if (!requestingUserChatMember.getIsAdmin()) {
            throw new IllegalArgumentException("Only admins can remove members from the chat");
        }

        // Проверяем, существует ли участник чата, которого нужно удалить
        ChatMemberId chatMemberIdToRemove = new ChatMemberId(chatId, id);
        if (!chatMemberRepository.existsById(chatMemberIdToRemove)) {
            throw new IllegalArgumentException("Chat member not found");
        }

        chatMemberRepository.deleteById(chatMemberIdToRemove);
    }
}