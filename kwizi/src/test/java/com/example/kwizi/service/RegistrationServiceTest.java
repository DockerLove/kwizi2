package com.example.kwizi.service;


import com.example.kwizi.DTO.request.RegistrationRequest;
import com.example.kwizi.exception.EmailAlreadyExistsException;
import com.example.kwizi.exception.UsernameAlreadyExistsException;
import com.example.kwizi.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private RegistrationService registrationService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private final String USERNAME = "testuser";
    private final String EMAIL = "test@example.com";
    private final String PASSWORD = "password123";

    // ===== ТЕСТЫ ДЛЯ УСПЕШНОЙ РЕГИСТРАЦИИ =====

    @Test
    void registerUser_WithValidRequest_ShouldRegisterUserSuccessfully() {
        // Arrange
        RegistrationRequest request = createValidRegistrationRequest();

        when(authenticationService.existsByUsername(USERNAME)).thenReturn(false);
        when(authenticationService.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // Act
        registrationService.registerUser(request);

        // Assert
        verify(authenticationService).registerUser(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getUsername()).isEqualTo(USERNAME);
        assertThat(savedUser.getEmail()).isEqualTo(EMAIL);
        assertThat(savedUser.getPassword()).isEqualTo(PASSWORD);
        assertThat(savedUser.getFirstName()).isEqualTo("John");
        assertThat(savedUser.getLastName()).isEqualTo("Doe");
        assertThat(savedUser.getBio()).isEqualTo("Test bio");

        verify(authenticationService).existsByUsername(USERNAME);
        verify(authenticationService).findByEmail(EMAIL);
    }

    @Test
    void registerUser_WithMinimalData_ShouldRegisterUserSuccessfully() {
        // Arrange
        RegistrationRequest request = createMinimalRegistrationRequest();

        when(authenticationService.existsByUsername(USERNAME)).thenReturn(false);
        when(authenticationService.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // Act
        registrationService.registerUser(request);

        // Assert
        verify(authenticationService).registerUser(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getUsername()).isEqualTo(USERNAME);
        assertThat(savedUser.getEmail()).isEqualTo(EMAIL);
        assertThat(savedUser.getPassword()).isEqualTo(PASSWORD);
        assertThat(savedUser.getFirstName()).isNull();
        assertThat(savedUser.getLastName()).isNull();
        assertThat(savedUser.getBio()).isNull();
    }

    // ===== ТЕСТЫ ДЛЯ ВАЛИДАЦИИ =====

    @Test
    void registerUser_WhenUsernameAlreadyExists_ShouldThrowException() {
        // Arrange
        RegistrationRequest request = createValidRegistrationRequest();

        when(authenticationService.existsByUsername(USERNAME)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> registrationService.registerUser(request))
                .isInstanceOf(UsernameAlreadyExistsException.class)
                .hasMessage("Пользователь с таким username уже существует");

        verify(authenticationService, never()).registerUser(any());
        verify(authenticationService, never()).findByEmail(any());
    }

    @Test
    void registerUser_WhenEmailAlreadyExists_ShouldThrowException() {
        // Arrange
        RegistrationRequest request = createValidRegistrationRequest();
        User existingUser = createTestUser();

        when(authenticationService.existsByUsername(USERNAME)).thenReturn(false);
        when(authenticationService.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser));

        // Act & Assert
        assertThatThrownBy(() -> registrationService.registerUser(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("Пользователь с таким email уже существует");

        verify(authenticationService, never()).registerUser(any());
    }

    @Test
    void registerUser_WhenBothUsernameAndEmailExist_ShouldThrowUsernameExceptionFirst() {
        // Arrange
        RegistrationRequest request = createValidRegistrationRequest();

        when(authenticationService.existsByUsername(USERNAME)).thenReturn(true);
        // findByEmail не должен вызываться если username уже существует

        // Act & Assert
        assertThatThrownBy(() -> registrationService.registerUser(request))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verify(authenticationService, never()).findByEmail(any());
        verify(authenticationService, never()).registerUser(any());
    }

    // ===== ТЕСТЫ ДЛЯ СОЗДАНИЯ ПОЛЬЗОВАТЕЛЯ =====

    @Test
    void createUserFromRequest_ShouldMapAllFieldsCorrectly() {
        // Arrange
        RegistrationRequest request = createValidRegistrationRequest();

        // Act (тестируем приватный метод через публичный)
        when(authenticationService.existsByUsername(USERNAME)).thenReturn(false);
        when(authenticationService.findByEmail(EMAIL)).thenReturn(Optional.empty());

        registrationService.registerUser(request);

        // Assert
        verify(authenticationService).registerUser(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser)
                .extracting(
                        User::getUsername,
                        User::getEmail,
                        User::getPassword,
                        User::getFirstName,
                        User::getLastName,
                        User::getBio
                )
                .containsExactly(
                        USERNAME,
                        EMAIL,
                        PASSWORD,
                        "John",
                        "Doe",
                        "Test bio"
                );
    }

    @Test
    void createUserFromRequest_WithNullFields_ShouldHandleCorrectly() {
        // Arrange
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername(USERNAME);
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        // firstName, lastName, bio остаются null

        when(authenticationService.existsByUsername(USERNAME)).thenReturn(false);
        when(authenticationService.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // Act
        registrationService.registerUser(request);

        // Assert
        verify(authenticationService).registerUser(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getFirstName()).isNull();
        assertThat(savedUser.getLastName()).isNull();
        assertThat(savedUser.getBio()).isNull();
    }

    // ===== ТЕСТЫ ДЛЯ ГРАНИЧНЫХ СЛУЧАЕВ =====

    @Test
    void registerUser_WithSpecialCharacters_ShouldHandleCorrectly() {
        // Arrange
        RegistrationRequest request = createRegistrationRequest(
                "user-name.test",
                "test+tag@example.com",
                "pass word!@#",
                "John-Michael",
                "O'Conner",
                "Bio with 🚀 emoji"
        );

        when(authenticationService.existsByUsername("user-name.test")).thenReturn(false);
        when(authenticationService.findByEmail("test+tag@example.com")).thenReturn(Optional.empty());

        // Act
        registrationService.registerUser(request);

        // Assert
        verify(authenticationService).registerUser(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getUsername()).isEqualTo("user-name.test");
        assertThat(savedUser.getEmail()).isEqualTo("test+tag@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("pass word!@#");
        assertThat(savedUser.getFirstName()).isEqualTo("John-Michael");
        assertThat(savedUser.getLastName()).isEqualTo("O'Conner");
        assertThat(savedUser.getBio()).isEqualTo("Bio with 🚀 emoji");
    }

    @Test
    void registerUser_WithEmptyBio_ShouldHandleCorrectly() {
        // Arrange
        RegistrationRequest request = createValidRegistrationRequest();
        request.setBio(""); // пустой bio

        when(authenticationService.existsByUsername(USERNAME)).thenReturn(false);
        when(authenticationService.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // Act
        registrationService.registerUser(request);

        // Assert
        verify(authenticationService).registerUser(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getBio()).isEmpty();
    }

    // ===== ТЕСТ НА ПОРЯДОК ВЫЗОВОВ =====

    @Test
    void registerUser_ShouldCallMethodsInCorrectOrder() {
        // Arrange
        RegistrationRequest request = createValidRegistrationRequest();

        when(authenticationService.existsByUsername(USERNAME)).thenReturn(false);
        when(authenticationService.findByEmail(EMAIL)).thenReturn(Optional.empty());

        // Act
        registrationService.registerUser(request);

        // Assert - проверяем порядок вызовов
        // 1. Проверка username
        // 2. Проверка email
        // 3. Регистрация пользователя
        verify(authenticationService).existsByUsername(USERNAME);
        verify(authenticationService).findByEmail(EMAIL);
        verify(authenticationService).registerUser(any(User.class));
    }

    // ===== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =====

    private RegistrationRequest createValidRegistrationRequest() {
        return createRegistrationRequest(
                USERNAME,
                EMAIL,
                PASSWORD,
                "John",
                "Doe",
                "Test bio"
        );
    }

    private RegistrationRequest createMinimalRegistrationRequest() {
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername(USERNAME);
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        return request;
    }

    private RegistrationRequest createRegistrationRequest(String username, String email, String password,
                                                          String firstName, String lastName, String bio) {
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setBio(bio);
        return request;
    }

    private User createTestUser() {
        User user = new User();
        user.setUsername(USERNAME);
        user.setEmail(EMAIL);
        return user;
    }
}