package com.example.kwizi.service;

import com.example.kwizi.DTO.request.AddChatMemberRequestDto;
import com.example.kwizi.DTO.request.CreateChatRequestDto;
import com.example.kwizi.model.Chat;
import com.example.kwizi.model.ChatMember;
import com.example.kwizi.model.ChatMember.ChatMemberId;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.ChatMemberRepository;
import com.example.kwizi.repository.ChatRepository;
import com.example.kwizi.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
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
    public void createChat(CreateChatRequestDto createChatRequestDto) {
        Chat chat = new Chat();
        chat.setGroupName(createChatRequestDto.getGroupName());

        User creator = userRepository.findById(createChatRequestDto.getCreatorId()).orElseThrow(() -> new IllegalArgumentException("Creator not found"));
        chat.setCreatedBy(creator);
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
        ChatMemberId chatMemberId = new ChatMemberId(chatId, userId);
        ChatMember chatMember = chatMemberRepository.findById(chatMemberId).orElseThrow(() -> new IllegalArgumentException("Chat member not found"));
        chatMember.setIsAdmin(true);
        chatMemberRepository.save(chatMember);
    }
}