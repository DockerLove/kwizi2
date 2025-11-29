package com.example.kwizi.service;

import com.example.kwizi.DTO.internal.MessageDto;
import com.example.kwizi.DTO.response.ChatHistoryResponse;
import com.example.kwizi.enums.ChatRole;
import com.example.kwizi.enums.ChatType;
import com.example.kwizi.exception.ChatNotFoundException;
import com.example.kwizi.exception.MessageService.MessageEditTimeExpiredException;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.model.Chat;
import com.example.kwizi.model.ChatMember;
import com.example.kwizi.model.Message;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.ChatMemberRepository;
import com.example.kwizi.repository.ChatRepository;
import com.example.kwizi.repository.MessageRepository;
import com.example.kwizi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatMemberRepository chatMemberRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChatMessageService chatMessageService;

    private User testUser;
    private User testRecipient;
    private Chat testChat;
    private Message testMessage;
    private ChatMember testChatMember;
    private MessageDto messageDto;

    @BeforeEach
    void setUp() {
        // Arrange common test data
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");

        testRecipient = new User();
        testRecipient.setId(2L);
        testRecipient.setUsername("testRecipient");

        testChat = new Chat();
        testChat.setId(1L);
        testChat.setChatType(ChatType.PRIVATE);

        testMessage = new Message();
        testMessage.setId(1L);
        testMessage.setChat(testChat);
        testMessage.setSender(testUser);
        testMessage.setText("Original text");
        testMessage.setCreatedAt(OffsetDateTime.now().minusHours(1));

        testChatMember = new ChatMember(testChat, testUser, ChatRole.MEMBER);

        messageDto = new MessageDto();
        messageDto.setChatId(1L);
        messageDto.setText("Test message");
    }

    // ✅ Тесты для sendMessage
    @Test
    void sendMessage_ShouldSuccessfullySendMessage_WhenValidInput() {
        // Arrange
        when(chatRepository.findById(1L)).thenReturn(Optional.of(testChat));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chatMemberRepository.existsByChatIdAndUserId(1L, 1L)).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

        // Act
        Message result = chatMessageService.sendMessage(messageDto, 1L);

        // Assert
        assertNotNull(result);
        verify(chatRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(chatMemberRepository).existsByChatIdAndUserId(1L, 1L);
        verify(messageRepository).save(any(Message.class));
        verify(chatService).updateChatActivity(1L);
    }

    @Test
    void sendMessage_ShouldThrowChatNotFoundException_WhenChatNotFound() {
        // Arrange
        when(chatRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ChatNotFoundException.class, () ->
                chatMessageService.sendMessage(messageDto, 1L));

        verify(userRepository, never()).findById(anyLong());
        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void sendMessage_ShouldThrowUserNotFoundException_WhenUserNotFound() {
        // Arrange
        when(chatRepository.findById(1L)).thenReturn(Optional.of(testChat));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () ->
                chatMessageService.sendMessage(messageDto, 1L));

        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void sendMessage_ShouldThrowSecurityException_WhenUserNotInChat() {
        // Arrange
        when(chatRepository.findById(1L)).thenReturn(Optional.of(testChat));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(chatMemberRepository.existsByChatIdAndUserId(1L, 1L)).thenReturn(false);

        // Act & Assert
        assertThrows(SecurityException.class, () ->
                chatMessageService.sendMessage(messageDto, 1L));

        verify(messageRepository, never()).save(any(Message.class));
    }

    // ✅ Тесты для sendPrivateMessage
    @Test
    void sendPrivateMessage_ShouldCreateNewChat_WhenNoExistingChat() {
        // Arrange
        MessageDto privateMessageDto = new MessageDto();
        privateMessageDto.setText("Private message");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testRecipient));
        when(chatMemberRepository.findPrivateChatIdByUserIds(1L, 2L)).thenReturn(Optional.empty());
        when(chatRepository.save(any(Chat.class))).thenReturn(testChat);
        when(chatMemberRepository.saveAll(anyList())).thenReturn(List.of(testChatMember));
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

        // Act
        Message result = chatMessageService.sendPrivateMessage(privateMessageDto, 1L, 2L);

        // Assert
        assertNotNull(result);
        verify(chatRepository).save(any(Chat.class));
        verify(chatMemberRepository).saveAll(anyList());
        verify(chatService).updateChatActivity(testChat.getId());
    }

    @Test
    void sendPrivateMessage_ShouldUseExistingChat_WhenChatExists() {
        // Arrange
        MessageDto privateMessageDto = new MessageDto();
        privateMessageDto.setText("Private message");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testRecipient));
        when(chatMemberRepository.findPrivateChatIdByUserIds(1L, 2L)).thenReturn(Optional.of(1L));
        when(chatRepository.findById(1L)).thenReturn(Optional.of(testChat));
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

        // Act
        Message result = chatMessageService.sendPrivateMessage(privateMessageDto, 1L, 2L);

        // Assert
        assertNotNull(result);
        verify(chatRepository, never()).save(any(Chat.class));
        verify(chatService).updateChatActivity(1L);
    }

    // ✅ Тесты для getChatHistory
    @Test
    void getChatHistory_ShouldReturnChatHistory_WhenUserHasAccess() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("createdAt")));
        Page<Message> messagePage = new PageImpl<>(List.of(testMessage));

        when(chatMemberRepository.existsByChatIdAndUsername(1L, "testUser")).thenReturn(true);
        when(messageRepository.findByChatId(1L, pageable)).thenReturn(messagePage);

        // Act
        Page<ChatHistoryResponse> result = chatMessageService.getChatHistory(1L, 0, 10, "desc", "testUser");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(chatMemberRepository).existsByChatIdAndUsername(1L, "testUser");
        verify(messageRepository).findByChatId(1L, pageable);
    }

    @Test
    void getChatHistory_ShouldThrowAccessDeniedException_WhenUserNoAccess() {
        // Arrange
        when(chatMemberRepository.existsByChatIdAndUsername(1L, "unauthorizedUser")).thenReturn(false);

        // Act & Assert
        assertThrows(AccessDeniedException.class, () ->
                chatMessageService.getChatHistory(1L, 0, 10, "desc", "unauthorizedUser"));

        verify(messageRepository, never()).findByChatId(anyLong(), any(Pageable.class));
    }

    // ✅ Тесты для editMessage
    @Test
    void editMessage_ShouldSuccessfullyEditMessage_WhenValidConditions() {
        // Arrange
        String newText = "Updated text";
        testMessage.setCreatedAt(OffsetDateTime.now().minusHours(12)); // Within 24 hours

        when(messageRepository.findById(1L)).thenReturn(Optional.of(testMessage));
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

        // Act
        chatMessageService.editMessage(1L, newText, "testUser");

        // Assert
        verify(messageRepository).save(testMessage);
        verify(notificationService).notifyMessageEdited(1L, 1L, newText, "testUser");
        assertTrue(testMessage.isEdited());
    }

    @Test
    void editMessage_ShouldThrowAccessDeniedException_WhenNotMessageOwner() {
        // Arrange
        when(messageRepository.findById(1L)).thenReturn(Optional.of(testMessage));

        // Act & Assert
        assertThrows(AccessDeniedException.class, () ->
                chatMessageService.editMessage(1L, "new text", "otherUser"));

        verify(messageRepository, never()).save(any(Message.class));
        verify(notificationService, never()).notifyMessageEdited(anyLong(), anyLong(), anyString(), anyString());
    }

    @Test
    void editMessage_ShouldThrowMessageEditTimeExpiredException_WhenTimeExpired() {
        // Arrange
        testMessage.setCreatedAt(OffsetDateTime.now().minusHours(25)); // More than 24 hours

        when(messageRepository.findById(1L)).thenReturn(Optional.of(testMessage));

        // Act & Assert
        assertThrows(MessageEditTimeExpiredException.class, () ->
                chatMessageService.editMessage(1L, "new text", "testUser"));

        verify(messageRepository, never()).save(any(Message.class));
    }

    // ✅ Тесты для deleteMessage
    @Test
    void deleteMessage_ShouldSuccessfullyDelete_WhenUserIsMessageSender() {
        // Arrange
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(testUser));
        when(messageRepository.findById(1L)).thenReturn(Optional.of(testMessage));
        when(chatMemberRepository.findByChatIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testChatMember));

        // Act
        chatMessageService.deleteMessage(1L, "testUser");

        // Assert
        verify(messageRepository).delete(testMessage);
        verify(notificationService).notifyMessageDeleted(1L, 1L, "testUser");
    }

    @Test
    void deleteMessage_ShouldSuccessfullyDelete_WhenUserIsAdmin() {
        // Arrange
        User adminUser = new User();
        adminUser.setId(3L);
        adminUser.setUsername("adminUser");

        ChatMember adminMember = new ChatMember(testChat, adminUser, ChatRole.ADMIN);

        when(userRepository.findByUsername("adminUser")).thenReturn(Optional.of(adminUser));
        when(messageRepository.findById(1L)).thenReturn(Optional.of(testMessage));
        when(chatMemberRepository.findByChatIdAndUserId(1L, 3L))
                .thenReturn(Optional.of(adminMember));

        // Act
        chatMessageService.deleteMessage(1L, "adminUser");

        // Assert
        verify(messageRepository).delete(testMessage);
        verify(notificationService).notifyMessageDeleted(1L, 1L, "adminUser");
    }

    @Test
    void deleteMessage_ShouldThrowAccessDeniedException_WhenNoPermissions() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(3L);
        otherUser.setUsername("otherUser");

        when(userRepository.findByUsername("otherUser")).thenReturn(Optional.of(otherUser));
        when(messageRepository.findById(1L)).thenReturn(Optional.of(testMessage));
        when(chatMemberRepository.findByChatIdAndUserId(1L, 3L))
                .thenReturn(Optional.of(testChatMember));

        // Act & Assert
        assertThrows(AccessDeniedException.class, () ->
                chatMessageService.deleteMessage(1L, "otherUser"));

        verify(messageRepository, never()).delete(any(Message.class));
        verify(notificationService, never()).notifyMessageDeleted(anyLong(), anyLong(), anyString());
    }

    // ✅ Тесты для getChatMembers
    @Test
    void getChatMembers_ShouldReturnMemberList_WhenChatExists() {
        // Arrange
        List<Long> expectedMembers = List.of(1L, 2L, 3L);

        when(chatRepository.existsById(1L)).thenReturn(true);
        when(chatMemberRepository.findUserIdsByChatId(1L)).thenReturn(expectedMembers);

        // Act
        List<Long> result = chatMessageService.getChatMembers(1L);

        // Assert
        assertEquals(expectedMembers, result);
        verify(chatRepository).existsById(1L);
        verify(chatMemberRepository).findUserIdsByChatId(1L);
    }

    @Test
    void getChatMembers_ShouldThrowChatNotFoundException_WhenChatNotExists() {
        // Arrange
        when(chatRepository.existsById(1L)).thenReturn(false);

        // Act & Assert
        assertThrows(ChatNotFoundException.class, () ->
                chatMessageService.getChatMembers(1L));

        verify(chatMemberRepository, never()).findUserIdsByChatId(anyLong());
    }

    // ✅ Тест для convertToChatHistoryResponse
    @Test
    void convertToChatHistoryResponse_ShouldConvertCorrectly() {
        // Act
        ChatHistoryResponse response = chatMessageService.convertToChatHistoryResponse(testMessage);

        // Assert
        assertEquals(testMessage.getId(), response.getMessageId());
        assertEquals(testMessage.getText(), response.getText());
        assertEquals(testMessage.getSender().getId(), response.getSenderId());
        assertEquals(testMessage.getSender().getUsername(), response.getSenderName());
        assertEquals(testMessage.getCreatedAt().toLocalDateTime(), response.getCreatedAt());
    }
}