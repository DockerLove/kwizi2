package com.example.kwizi.service;

import com.example.kwizi.DTO.internal.MessageDto;
import com.example.kwizi.model.Chat;
import com.example.kwizi.model.ChatMember;
import com.example.kwizi.model.Message;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.ChatMemberRepository;
import com.example.kwizi.repository.ChatRepository;
import com.example.kwizi.repository.MessageRepository;
import com.example.kwizi.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private ChatMemberRepository chatMemberRepository;

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private ChatMessageService chatMessageService;

    private User testUser;
    private User testRecipient;
    private MessageDto testMessageDto;


    @BeforeEach
    void setU() {
        testMessageDto = new MessageDto();
        testMessageDto.setId(1L);
        testMessageDto.setText("Test message");
    }


    @BeforeEach
    void setUpp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
    }

    @BeforeEach
    void setUp() {
        testRecipient = new User();
        testRecipient.setId(1L);
        testRecipient.setUsername("testuser");
        testRecipient.setEmail("test@example.com");
        testRecipient.setFirstName("Test");
        testRecipient.setLastName("User");
    }

    @Test
    void getChatMembers_WhenChatExists_ReturnsMemberIds() {
        // Arrange
        Long chatId = 1L;
        List<Long> expectedMemberIds = Arrays.asList(101L, 102L);

        when(chatMemberRepository.findUserIdsByChatId(chatId))
                .thenReturn(expectedMemberIds);

        // Act
        List<Long> result = chatMessageService.getChatMembers(chatId);

        // Assert
        assertEquals(expectedMemberIds, result);
        verify(chatMemberRepository, times(1)).findUserIdsByChatId(chatId);
    }

    @Test
    void getChatMembers_WhenNoMembers_ReturnsEmptyList() {
        // Arrange
        Long chatId = 2L;

        when(chatMemberRepository.findUserIdsByChatId(chatId))
                .thenReturn(List.of());

        // Act
        List<Long> result = chatMessageService.getChatMembers(chatId);

        // Assert
        assertTrue(result.isEmpty());
        verify(chatMemberRepository, times(1)).findUserIdsByChatId(chatId);
    }
    @Test
    void getOrCreateChat_chatExists_returnsExistingChat() {
        // Arrange
        Long chatId = 123L;
        Chat existingChat = new Chat();
        existingChat.setId(chatId);
        existingChat.setCreatedBy(testUser); // Чат уже создан другим пользователем

        when(chatRepository.findById(chatId)).thenReturn(Optional.of(existingChat));

        // Act
        Chat resultChat = chatMessageService.getOrCreateChat(chatId, testUser);

        // Assert
        assertEquals(existingChat, resultChat);
        assertEquals(testUser, existingChat.getCreatedBy()); // Проверяем, что создатель чата - testUser
        verify(chatRepository, never()).save(any(Chat.class)); // Убеждаемся, что сохранение не вызывалось
    }

    @Test
    void getOrCreateChat_chatExistsButCreatedByIsNull_setsCreatedBy() {
        // Arrange
        Long chatId = 789L;
        Chat existingChat = new Chat();
        existingChat.setId(chatId);
        existingChat.setCreatedBy(null); // createdBy не установлен

        when(chatRepository.findById(chatId)).thenReturn(Optional.of(existingChat));
        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Chat resultChat = chatMessageService.getOrCreateChat(chatId, testUser);

        // Assert
        assertEquals(existingChat, resultChat);
        assertEquals(testUser, resultChat.getCreatedBy());
        verify(chatRepository).save(existingChat);
    }

    @Test
    void sendMessage_chatExistsAndSenderValid_returnsSavedMessage() {
        // Given
        Long chatId = 1L;
        Long senderId = 100L;
        String messageText = "Hello, world!";

        MessageDto messageDto = new MessageDto();
        messageDto.setChatId(chatId);
        messageDto.setId(1L);
        messageDto.setText(messageText);
        messageDto.setCreatedAt(OffsetDateTime.now().toLocalDateTime());
        messageDto.setSenderId(testUser.getId());

        Chat existingChat = new Chat();
        existingChat.setId(chatId);
        existingChat.setCreatedBy(testUser);

        when(chatRepository.findById(chatId)).thenReturn(Optional.of(existingChat));
        when(userRepository.findById(senderId)).thenReturn(Optional.of(testUser));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Message result = chatMessageService.sendMessage(messageDto, senderId);

        // Then
        assertNotNull(result);
        assertEquals(messageText, result.getText());
        assertEquals(testUser, result.getSender());
        assertEquals(existingChat, result.getChat());
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void sendMessage_senderNotFound_throwsException() {
        // Given
        Long chatId = 3L;
        Long userId = 1L;
        MessageDto messageDto = new MessageDto();
        messageDto.setChatId(chatId);
        messageDto.setText("Test message");

        when(chatRepository.findById(chatId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Then
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            chatMessageService.sendMessage(messageDto, userId);
        });

        assertEquals("Пользователь не найден", exception.getMessage());
    }

    @Test
    void sendMessage_chatCreationFailed_throwsException() {
        // Given
        Long chatId = 4L;
        Long senderId = 400L;
        MessageDto messageDto = new MessageDto();
        messageDto.setChatId(chatId);
        messageDto.setText("Test message");

        when(chatRepository.findById(chatId))
                .thenReturn(Optional.empty())  // Первый вызов - чата нет
                .thenReturn(Optional.empty()); // Второй вызов - чат не создался

        when(userRepository.findById(senderId)).thenReturn(Optional.of(testUser));

        // Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            chatMessageService.sendMessage(messageDto, senderId);
        });
        assertEquals("Чат не найден", exception.getMessage());
    }

    @Test
    void sendMessage_ShouldThrowRuntimeException_WhenExceptionOccurs() {
        // Arrange
        MessageDto messageDto = new MessageDto();
        messageDto.setChatId(1L);
        messageDto.setText("Test message");
        Long senderId = 2L;

        when(chatRepository.findById(messageDto.getChatId())).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            chatMessageService.sendMessage(messageDto, senderId);
        });
        assertEquals("Database error", exception.getCause().getMessage());

        verify(chatRepository).findById(messageDto.getChatId());
        verify(userRepository, never()).findById(any());
        verify(messageRepository, never()).save(any(Message.class));
    }


    // 1. Тест: успешная отправка в существующий чат
    @Test
    void sendPrivateMessage_existingChat_returnsMessage() {
        // Given
        Long existingChatId = 10L;
        Chat existingChat = new Chat();
        existingChat.setId(existingChatId);
        existingChat.setCreatedBy(testUser);

        when(chatMemberRepository.findPrivateChatIdByUserIds(1L, 2L))
                .thenReturn(Optional.of(existingChatId));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testRecipient));
        when(chatRepository.findById(existingChatId)).thenReturn(Optional.of(existingChat));
        when(messageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Message result = chatMessageService.sendPrivateMessage(testMessageDto, 1L, 2L);

        // Then
        assertNotNull(result);
        assertEquals("Test message", result.getText());
        assertEquals(existingChat, result.getChat());
        assertEquals(testUser, result.getSender());
        verify(chatRepository, never()).save(any()); // Чат не должен сохраняться заново
    }

    @Test
    void sendPrivateMessage_newChat_createsChatAndMembers() {

        Chat newChat = new Chat();
        newChat.setId(20L);
        newChat.setCreatedBy(testUser);
        when(chatMemberRepository.findPrivateChatIdByUserIds(1L, 2L))
                .thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testRecipient));
        when(chatRepository.save(any())).thenReturn(newChat);
        when(messageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Захватываем List<ChatMember>
        ArgumentCaptor<List<ChatMember>> memberCaptor = ArgumentCaptor.forClass(List.class);

        Message result = chatMessageService.sendPrivateMessage(testMessageDto, 1L, 2L);

        verify(chatRepository).save(any(Chat.class)); // Чат должен быть сохранен
        verify(chatMemberRepository).saveAll(memberCaptor.capture());

        // Получаем List<ChatMember> из ArgumentCaptor
        List<ChatMember> savedMembers = memberCaptor.getValue();

        assertEquals(2, savedMembers.size());
        assertTrue(savedMembers.stream().anyMatch(m -> m.getUser().equals(testUser)));
        assertTrue(savedMembers.stream().anyMatch(m -> m.getUser().equals(testRecipient)));
    }
    // 3. Тест: отправитель не найден
    @Test
    void sendPrivateMessage_senderNotFound_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            chatMessageService.sendPrivateMessage(testMessageDto, 1L, 2L);
        });
    }

    @Test
    void sendPrivateMessage_recipientNotFound_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            chatMessageService.sendPrivateMessage(testMessageDto, 1L, 2L);
        });
    }

    @Test
    void sendPrivateMessage_setsCorrectTimestamps() {
        when(chatMemberRepository.findPrivateChatIdByUserIds(1L, 2L))
                .thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testRecipient));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testRecipient));
        when(chatRepository.save(any())).thenAnswer(invocation -> {
            Chat c = invocation.getArgument(0);
            c.setId(30L);
            return c;
        });
        when(messageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Message result = chatMessageService.sendPrivateMessage(testMessageDto, 1L, 2L);

        assertNotNull(result.getCreatedAt());
        assertTrue(result.getCreatedAt().isBefore(OffsetDateTime.now().plusSeconds(1)));
    }
}