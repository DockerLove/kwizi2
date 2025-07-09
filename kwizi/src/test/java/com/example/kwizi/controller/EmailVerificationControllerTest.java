package com.example.kwizi.controller;
import com.example.kwizi.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class EmailVerificationControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private EmailVerificationController emailVerificationController;

    @Test
    void verifyEmail_ShouldReturnOk_WhenEmailVerifiedSuccessfully() {
        // Arrange
        String token = "testToken";
        doNothing().when(authenticationService).verifyEmail(token);

        // Act
        ResponseEntity<String> response = emailVerificationController.verifyEmail(token);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.TEXT_HTML, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals("Email успешно подтвержден! Можете закрыть эту страницу.", response.getBody());
        verify(authenticationService).verifyEmail(token);
    }

    @Test
    void verifyEmail_ShouldReturnBadRequest_WhenIllegalArgumentExceptionIsThrown() {
        // Arrange
        String token = "invalidToken";
        String errorMessage = "Invalid token";
        doThrow(new IllegalArgumentException(errorMessage)).when(authenticationService).verifyEmail(token);

        // Act
        ResponseEntity<String> response = emailVerificationController.verifyEmail(token);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(MediaType.TEXT_HTML, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertEquals("Ошибка: " + errorMessage, response.getBody());
        verify(authenticationService).verifyEmail(token);
    }
}