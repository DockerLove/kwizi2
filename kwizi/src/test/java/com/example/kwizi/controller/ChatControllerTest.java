package com.example.kwizi.controller;
import com.example.kwizi.DTO.request.AddChatMemberRequestDto;
import com.example.kwizi.DTO.request.CreateGroupChatRequest;
import com.example.kwizi.DTO.request.CreatePrivateChatRequest;
import com.example.kwizi.DTO.response.ApiResponse;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.model.User;
import com.example.kwizi.security.UserDetailsImpl;
import com.example.kwizi.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChatController chatController;


    @Test
    void createGroupChat_ShouldReturnOk_WhenChatCreatedSuccessfully() {
        // Arrange
        CreateGroupChatRequest createChatRequest = new CreateGroupChatRequest();
        createChatRequest.setGroupName("Test Group");
        String username = "testUser";

        // Mock UserDetailsImpl
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setUsername("testUser");
        user.setPassword("password");
        UserDetailsImpl userDetails = new UserDetailsImpl(user);

        // Act
        doNothing().when(chatService).createGroupChat(createChatRequest, username); // Use doNothing since createGroupChat is void
        ResponseEntity<?> response = chatController.createGroupChat(createChatRequest, userDetails);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Групповой чат успешно создан", ((ApiResponse<?>) response.getBody()).getMessage());
        verify(chatService).createGroupChat(createChatRequest, username);
    }

    @Test
    void createPrivateChat_ShouldReturnOk_WhenChatCreatedSuccessfully() {
        // Arrange
        CreatePrivateChatRequest createPrivateChatRequest = new CreatePrivateChatRequest();
        createPrivateChatRequest.setRecipientUsername("recipientUser");
        String username = "testUser";

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setUsername("testUser");
        user.setPassword("password");
        UserDetailsImpl userDetails = new UserDetailsImpl(user);

        // Act
        doNothing().when(chatService).createPrivateChat(createPrivateChatRequest, username);
        ResponseEntity<?> response = chatController.createPrivateChat(createPrivateChatRequest, userDetails);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Приватный чат успешно создан", ((ApiResponse<?>) response.getBody()).getMessage());
        verify(chatService).createPrivateChat(createPrivateChatRequest, username);
    }

    @Test
    void addChatMember_ShouldReturnOk_WhenMemberAddedSuccessfully() {
        // Arrange
        Long chatId = 123L;
        AddChatMemberRequestDto addChatMemberRequestDto = new AddChatMemberRequestDto();
        addChatMemberRequestDto.setUserId(456L);

        // Act
        doNothing().when(chatService).addChatMember(any(AddChatMemberRequestDto.class)); // use any() here
        ResponseEntity<?> response = chatController.addChatMember(chatId, addChatMemberRequestDto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Пользователь успешно добавлен в чат", ((ApiResponse<?>) response.getBody()).getMessage());
        verify(chatService).addChatMember(addChatMemberRequestDto); // Verify with the exact object
    }

    @Test
    void setAdmin_ShouldReturnSuccessResponse() {
        // Arrange
        Long chatId = 1L;
        Long userId = 2L;
        Long currentUserId = 3L;
        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
        when(userDetails.getId()).thenReturn(currentUserId);

        // Act
        ResponseEntity<?> response = chatController.setAdmin(chatId, userId, userDetails);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponse<?> apiResponse = (ApiResponse<?>) response.getBody();
        assertNotNull(apiResponse);
        assertEquals("Пользователь успешно назначен админом", apiResponse.getMessage());
        assertNull(apiResponse.getData());

        verify(chatService).setAdmin(chatId, userId, currentUserId);
    }

    @Test
    void setAdmin_ShouldHandleException() {
        // Arrange
        Long chatId = 1L;
        Long userId = 2L;
        Long currentUserId = 3L;
        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
        when(userDetails.getId()).thenReturn(currentUserId);

        doThrow(new IllegalArgumentException("Ошибка назначения"))
                .when(chatService).setAdmin(chatId, userId, currentUserId);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> chatController.setAdmin(chatId, userId, userDetails));
    }

    @Test
    void removeChatMember_ShouldReturnSuccessResponse() {
        // Arrange
        Long chatId = 1L;
        Long userId = 2L;
        Long currentUserId = 3L;
        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
        when(userDetails.getId()).thenReturn(currentUserId);

        // Act
        ResponseEntity<?> response = chatController.removeChatMember(chatId, userId, userDetails);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        ApiResponse<?> apiResponse = (ApiResponse<?>) response.getBody();
        assertNotNull(apiResponse);
        assertEquals("Пользователь удален из чата", apiResponse.getMessage());
        assertNull(apiResponse.getData());

        verify(chatService).removeChatMember(chatId, userId, currentUserId);
    }

    @Test
    void removeChatMember_ShouldHandleException() {
        // Arrange
        Long chatId = 1L;
        Long userId = 2L;
        Long currentUserId = 3L;
        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
        when(userDetails.getId()).thenReturn(currentUserId);

        doThrow(new UserNotFoundException("Пользователь не найден"))
                .when(chatService).removeChatMember(chatId, userId, currentUserId);

        // Act & Assert
        assertThrows(UserNotFoundException.class,
                () -> chatController.removeChatMember(chatId, userId, userDetails));
    }
}