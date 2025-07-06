package com.example.kwizi.service;
import com.example.kwizi.model.Chat;
import com.example.kwizi.model.Message;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.ChatMemberRepository;
import com.example.kwizi.repository.ChatRepository;
import com.example.kwizi.repository.MessageRepository;
import com.example.kwizi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
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
    void getChatHistory_WhenChatExists_ReturnsMessages() {
        // Arrange
        Long chatId = 1L;
        Chat chat = new Chat();
        chat.setId(chatId);

        Message message1 = new Message();
        message1.setId(1L);
        message1.setText("Hello");
        message1.setCreatedAt(OffsetDateTime.now());

        Message message2 = new Message();
        message2.setId(2L);
        message2.setText("World");
        message2.setCreatedAt(OffsetDateTime.now());

        List<Message> expectedMessages = Arrays.asList(message1, message2);

        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(messageRepository.findByChatIdAndIsDeleted(chatId, false)).thenReturn(expectedMessages);

        // Act
        List<Message> result = chatMessageService.getChatHistory(chatId);

        // Assert
        assertEquals(expectedMessages.size(), result.size());
        assertEquals(expectedMessages, result);
        verify(chatRepository, times(1)).findById(chatId);
        verify(messageRepository, times(1)).findByChatIdAndIsDeleted(chatId, false);
    }

    @Test
    void getChatHistory_WhenChatNotExists_ThrowsException() {
        // Arrange
        Long nonExistentChatId = 999L;
        when(chatRepository.findById(nonExistentChatId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> chatMessageService.getChatHistory(nonExistentChatId)
        );

        assertEquals("Чат не найден", exception.getMessage());
        verify(chatRepository, times(1)).findById(nonExistentChatId);
        verify(messageRepository, never()).findByChatIdAndIsDeleted(any(), anyBoolean());
    }

    @Test
    void getChatHistory_WhenNoMessages_ReturnsEmptyList() {

        Long chatId = 2L;
        Chat chat = new Chat();
        chat.setId(chatId);

        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(messageRepository.findByChatIdAndIsDeleted(chatId, false)).thenReturn(List.of());

        List<Message> result = chatMessageService.getChatHistory(chatId);

        assertTrue(result.isEmpty());
        verify(chatRepository, times(1)).findById(chatId);
        verify(messageRepository, times(1)).findByChatIdAndIsDeleted(chatId, false);
    }

}