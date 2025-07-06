package com.example.kwizi.service;

import com.example.kwizi.DTO.request.RegistrationRequest;
import com.example.kwizi.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RegistrationServiceTest {

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private RegistrationService registrationService;

    private RegistrationRequest createValidRequest() {
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("secure123");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setBio("New user bio");
        return request;
    }

    @Test
    void registerUser_WhenUsernameExists_ThrowsException() {
        // Arrange
        RegistrationRequest request = createValidRequest();
        when(authenticationService.findByUsername(request.getUsername()))
                .thenReturn(Optional.of(new User())); // Имитируем существующего пользователя

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> registrationService.registerUser(request)
        );
        assertEquals("Пользователь с таким username уже есть.", exception.getMessage());
        verify(authenticationService, never()).registerUser(any());
    }

    @Test
    void registerUser_WhenEmailExists_ThrowsException() {
        // Arrange
        RegistrationRequest request = createValidRequest();
        when(authenticationService.findByUsername(request.getUsername())).thenReturn(Optional.empty());
        when(authenticationService.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(new User())); // Имитируем существующий email

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> registrationService.registerUser(request)
        );
        assertEquals("Пользователь с таким email уже есть.", exception.getMessage());
        verify(authenticationService, never()).registerUser(any());
    }

    @Test
    void registerUser_WhenValidRequest_RegistersSuccessfully() {
        // Arrange
        RegistrationRequest request = createValidRequest();
        when(authenticationService.findByUsername(request.getUsername())).thenReturn(Optional.empty());
        when(authenticationService.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        assertDoesNotThrow(() -> registrationService.registerUser(request));
        verify(authenticationService).registerUser(any(User.class));
    }
}
