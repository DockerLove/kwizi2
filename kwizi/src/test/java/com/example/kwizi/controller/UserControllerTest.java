package com.example.kwizi.controller;

import com.example.kwizi.DTO.request.*;
import com.example.kwizi.DTO.response.ApiResponse;
import com.example.kwizi.DTO.response.UserProfileResponse;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.model.User;
import com.example.kwizi.security.JwtUtils;
import com.example.kwizi.security.UserDetailsImpl;
import com.example.kwizi.service.AuthenticationService;
import com.example.kwizi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private UserService userService;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private UserController userController;


    @Test
    void changePassword_ShouldReturnOk_WhenPasswordChangedSuccessfully() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        String username = "testUser";
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(authentication.getName()).thenReturn(username);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Act
        doNothing().when(authenticationService).changePassword(username, request);
        ResponseEntity<?> response = userController.changePassword(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Password changed successfully", response.getBody());
        verify(authenticationService).changePassword(username, request);
    }

    @Test
    void changePassword_ShouldReturnUnauthorized_WhenInvalidOldPassword() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        String username = "testUser";
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(authentication.getName()).thenReturn(username);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Act
        doThrow(new BadCredentialsException("Invalid old password"))
                .when(authenticationService).changePassword(username, request);
        ResponseEntity<?> response = userController.changePassword(request);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid old password", response.getBody());
        verify(authenticationService).changePassword(username, request);
    }

    @Test
    void changePassword_ShouldReturnInternalServerError_WhenOtherException() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        String username = "testUser";
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(authentication.getName()).thenReturn(username);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Act
        doThrow(new RuntimeException("Some server error"))
                .when(authenticationService).changePassword(username, request);
        ResponseEntity<?> response = userController.changePassword(request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Internal server error", response.getBody());
        verify(authenticationService).changePassword(username, request);
    }

    @Test
    void getUserByUsername_ShouldReturnUser_WhenUserExists() {
        // Arrange
        String username = "testUser";
        User user = new User();
        user.setUsername(username);
        when(authenticationService.findByUsername(username)).thenReturn(Optional.of(user));

        // Act
        ResponseEntity<ApiResponse<User>> response = userController.getUserByUsername(username);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Пользователь найден", response.getBody().getMessage());
        assertEquals(user, response.getBody().getData());
        verify(authenticationService).findByUsername(username);
    }

    @Test
    void getUserByUsername_ShouldReturnNotFound_WhenUserDoesNotExist() {
        // Arrange
        String username = "testUser";
        when(authenticationService.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> userController.getUserByUsername(username));
        verify(authenticationService).findByUsername(username);
    }

    @Test
    void verifyUserEmail_ShouldReturnOk() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setUsername("testUser");
        user.setPassword("password");
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        // Act
        ResponseEntity<ApiResponse<String>> response = userController.verifyUserEmail(userDetails);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Email успешно подтвержден", response.getBody().getMessage());
        verify(userService).verifyUserEmail(userDetails.getId());
    }

    @Test
    void sendVerificationEmail_ShouldReturnOk() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setUsername("testUser");
        user.setPassword("password");
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        // Act
        ResponseEntity<ApiResponse<String>> response = userController.sendVerificationEmail(userDetails);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Письмо для подтверждения email отправлено", response.getBody().getMessage());
        verify(authenticationService).sendVerificationEmail(userDetails.getId());
    }

    @Test
    void updateBio_ShouldReturnOk() {
        // Arrange
        UpdateBioRequest request = new UpdateBioRequest();
        request.setBio("New bio");
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setUsername("testUser");
        user.setPassword("password");
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        // Act
        ResponseEntity<?> response = userController.updateBio(request, userDetails);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Bio успешно обновлен", ((ApiResponse<?>) response.getBody()).getMessage());
        verify(userService).updateBio(userDetails.getId(), request.getBio());
    }

    @Test
    void updateFirstName_ShouldReturnOk() {
        // Arrange
        UpdateFirstNameRequest request = new UpdateFirstNameRequest();
        request.setFirstName("New First Name");
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setUsername("testUser");
        user.setPassword("password");
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        // Act
        ResponseEntity<?> response = userController.updateFirstName(request, userDetails);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Имя успешно обновлено", ((ApiResponse<?>) response.getBody()).getMessage());
        verify(userService).updateFirstName(userDetails.getId(), request.getFirstName());
    }

    @Test
    void getUserProfile_ShouldReturnOk() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setUsername("testUser");
        user.setPassword("password");
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        UserProfileResponse profile = new UserProfileResponse(1L, "firstName", "lastName", "testUser", "bio", "test@example.com");
        when(userService.getUserProfile(userDetails.getId())).thenReturn(profile);

        // Act
        ResponseEntity<ApiResponse<UserProfileResponse>> response = userController.getUserProfile(userDetails);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Профиль успешно загружен", response.getBody().getMessage());
        assertEquals(profile, response.getBody().getData());
        verify(userService).getUserProfile(userDetails.getId());
    }

    @Test
    void getEmailVerified_ShouldReturnOk() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setUsername("testUser");
        user.setPassword("password");
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        when(userService.getEmailVerified(userDetails.getId())).thenReturn(true);

        // Act
        ResponseEntity<ApiResponse<Boolean>> response = userController.getEmailVerified(userDetails);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Получен статус проверки", response.getBody().getMessage());
        assertEquals(true, response.getBody().getData());
        verify(userService).getEmailVerified(userDetails.getId());
    }

    @Test
    void updateLastName_ShouldReturnOk() {
        // Arrange
        UpdateLastNameRequest request = new UpdateLastNameRequest();
        request.setLastName("New Last Name");
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setUsername("testUser");
        user.setPassword("password");
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        // Act
        ResponseEntity<ApiResponse<String>> response = userController.updateLastName(request, userDetails);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Фамилия успешно обновлена", ((ApiResponse<?>) response.getBody()).getMessage());
        verify(userService).updateLastName(userDetails.getId(), request.getLastName());
    }

    @Test
    void updateUsername_ShouldReturnOk() {
        // Arrange
        UpdateUsernameRequest request = new UpdateUsernameRequest();
        request.setUsername("newUsername");
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setUsername("testUser");
        user.setPassword("password");
        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        String token = "new_token";
        when(userService.updateUsername(userDetails.getId(), request.getUsername())).thenReturn(token);

        // Act
        ResponseEntity<ApiResponse<Map<String, String>>> response = userController.updateUsername(request, userDetails);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Никнейм успешно обновлен", ((ApiResponse<?>) response.getBody()).getMessage());
        Map<String, String> expectedBody = Map.of("token", token);
        assertEquals(expectedBody, response.getBody().getData());
        verify(userService).updateUsername(userDetails.getId(), request.getUsername());
    }
}