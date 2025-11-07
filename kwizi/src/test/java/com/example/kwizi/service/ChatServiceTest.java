package com.example.kwizi.service;

import com.example.kwizi.DTO.request.CreateGroupChatRequest;
import com.example.kwizi.DTO.request.CreatePrivateChatRequest;
import com.example.kwizi.model.Chat;
import com.example.kwizi.model.ChatMember;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.ChatMemberRepository;
import com.example.kwizi.repository.ChatRepository;
import com.example.kwizi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private ChatMemberRepository chatMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatService chatService;

    // Тесты для createGroupChat
    @Test
    void createGroupChat_ShouldCreateChatAndAddMembers() {
        // Arrange
        CreateGroupChatRequest request = new CreateGroupChatRequest();
        request.setGroupName("Test Group");
        request.setInitialMemberIds(Arrays.asList(2L, 3L));

        User creator = new User();
        creator.setId(1L);
        creator.setUsername("creator");

        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));
        when(userRepository.findById(2L)).thenReturn(Optional.of(new User()));
        when(userRepository.findById(3L)).thenReturn(Optional.of(new User()));

        // Act
        chatService.createGroupChat(request, "creator");

        // Assert
        verify(chatRepository).save(any(Chat.class));
        verify(chatMemberRepository, times(3)).save(any(ChatMember.class));
    }

    @Test
    void createGroupChat_ShouldThrowWhenDuplicateMemberIds() {
        CreateGroupChatRequest request = new CreateGroupChatRequest();
        request.setInitialMemberIds(Arrays.asList(2L, 2L));

        assertThrows(IllegalArgumentException.class,
                () -> chatService.createGroupChat(request, "creator"));
    }

    @Test
    void createGroupChat_ShouldThrowWhenCreatorInMemberList() {
        CreateGroupChatRequest request = new CreateGroupChatRequest();
        request.setInitialMemberIds(Arrays.asList(1L));

        User creator = new User();
        creator.setId(1L);

        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));

        assertThrows(IllegalArgumentException.class,
                () -> chatService.createGroupChat(request, "creator"));
    }

    // Тесты для createPrivateChat
    @Test
    void createPrivateChat_ShouldCreateChatWithTwoMembers() {
        // Arrange
        CreatePrivateChatRequest request = new CreatePrivateChatRequest();
        request.setRecipientUsername("recipient");

        User creator = new User();
        creator.setId(1L);
        creator.setUsername("creator");

        User recipient = new User();
        recipient.setId(2L);
        recipient.setUsername("recipient");

        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));
        when(userRepository.findByUsername("recipient")).thenReturn(Optional.of(recipient));
        when(chatMemberRepository.findPrivateChatIdByUserIds(1L, 2L)).thenReturn(Optional.empty());

        // Act
        chatService.createPrivateChat(request, "creator");

        // Assert
        verify(chatRepository).save(any(Chat.class));
        verify(chatMemberRepository, times(2)).save(any(ChatMember.class));
    }

    @Test
    void createPrivateChat_ShouldThrowWhenChatExists() {
        CreatePrivateChatRequest request = new CreatePrivateChatRequest();
        request.setRecipientUsername("recipient");

        User creator = new User();
        creator.setId(1L);

        User recipient = new User();
        recipient.setId(2L);

        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));
        when(userRepository.findByUsername("recipient")).thenReturn(Optional.of(recipient));
        when(chatMemberRepository.findPrivateChatIdByUserIds(1L, 2L)).thenReturn(Optional.of(1L));

        assertThrows(IllegalArgumentException.class,
                () -> chatService.createPrivateChat(request, "creator"));
    }

    @Test
    void createPrivateChat_ShouldThrowWhenSelfChat() {
        CreatePrivateChatRequest request = new CreatePrivateChatRequest();
        request.setRecipientUsername("creator");

        User creator = new User();
        creator.setId(1L);

        when(userRepository.findByUsername("creator")).thenReturn(Optional.of(creator));

        assertThrows(IllegalArgumentException.class,
                () -> chatService.createPrivateChat(request, "creator"));
    }

    // Тесты для addChatMember

    // Тесты для setAdmin
    @Test
    void setAdmin_ShouldPromoteUserToAdmin() {
        // Arrange
        Chat chat = new Chat();
        chat.setGroup(true);

        ChatMember requester = new ChatMember();
        requester.setIsAdmin(true);

        ChatMember memberToPromote = new ChatMember();
        memberToPromote.setIsAdmin(false);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(chatMemberRepository.findByChatIdAndUserId(1L, 1L)).thenReturn(Optional.of(requester));
        when(chatMemberRepository.findByChatIdAndUserId(1L, 2L)).thenReturn(Optional.of(memberToPromote));

        // Act
        chatService.setAdmin(1L, 2L, 1L);

        // Assert
        assertTrue(memberToPromote.getIsAdmin());
        verify(chatMemberRepository).save(memberToPromote);
    }

    @Test
    void setAdmin_ShouldThrowWhenNotGroupChat() {
        Chat chat = new Chat();
        chat.setGroup(false);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        assertThrows(IllegalArgumentException.class,
                () -> chatService.setAdmin(1L, 2L, 1L));
    }

    @Test
    void setAdmin_ShouldThrowWhenRequesterNotAdmin() {
        Chat chat = new Chat();
        chat.setGroup(true);

        ChatMember requester = new ChatMember();
        requester.setIsAdmin(false);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(chatMemberRepository.findByChatIdAndUserId(1L, 1L)).thenReturn(Optional.of(requester));

        assertThrows(IllegalArgumentException.class,
                () -> chatService.setAdmin(1L, 2L, 1L));
    }

    // Тесты для removeChatMember
    @Test
    void removeChatMember_ShouldRemoveMemberFromGroupChat() {
        // Arrange
        Long chatId = 1L;
        Long requesterId = 1L;
        Long userIdToRemove = 2L;

        Chat chat = new Chat();
        chat.setGroup(true);

        User requester = new User();
        requester.setId(requesterId);

        User userToRemove = new User();
        userToRemove.setId(userIdToRemove);

        ChatMember requesterMember = new ChatMember();
        requesterMember.setIsAdmin(true);

        ChatMember.ChatMemberId requesterMemberId = new ChatMember.ChatMemberId(chatId, requesterId);
        ChatMember.ChatMemberId toRemoveMemberId = new ChatMember.ChatMemberId(chatId, userIdToRemove);

        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(userRepository.findById(userIdToRemove)).thenReturn(Optional.of(userToRemove));
        when(chatMemberRepository.findById(requesterMemberId))
                .thenReturn(Optional.of(requesterMember));
        when(chatMemberRepository.existsById(toRemoveMemberId))
                .thenReturn(true);

        // Act
        chatService.removeChatMember(chatId, userIdToRemove, requesterId);

        // Assert
        verify(chatMemberRepository).deleteById(toRemoveMemberId);
    }

    @Test
    void removeChatMember_ShouldThrowWhenPrivateChat() {
        Chat chat = new Chat();
        chat.setGroup(false);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        assertThrows(IllegalArgumentException.class,
                () -> chatService.removeChatMember(1L, 2L, 1L));
    }

    @Test
    void removeChatMember_ShouldThrowWhenRequesterNotAdmin() {
        // Arrange
        Long chatId = 1L;
        Long userIdToRemove = 2L;
        Long requestingUserId = 3L;

        Chat chat = new Chat();
        chat.setId(chatId);
        chat.setGroup(true); // It's a group chat

        User userToRemove = new User();
        userToRemove.setId(userIdToRemove);

        User requestingUser = new User();
        requestingUser.setId(requestingUserId);

        ChatMember.ChatMemberId requestingUserChatMemberId = new ChatMember.ChatMemberId(chatId, requestingUserId);
        ChatMember requestingUserChatMember = new ChatMember();
        requestingUserChatMember.setId(requestingUserChatMemberId);
        requestingUserChatMember.setIsAdmin(false); // The requester is NOT an admin

        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(userRepository.findById(userIdToRemove)).thenReturn(Optional.of(userToRemove));
        when(userRepository.findById(requestingUserId)).thenReturn(Optional.of(requestingUser));
        when(chatMemberRepository.findById(new ChatMember.ChatMemberId(chatId, requestingUserId))).thenReturn(Optional.of(requestingUserChatMember));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            chatService.removeChatMember(chatId, userIdToRemove, requestingUserId);
        });

        // Assert that the exception message is as expected
        assertEquals("Только админ может удалять других пользователей", exception.getMessage());
    }
}