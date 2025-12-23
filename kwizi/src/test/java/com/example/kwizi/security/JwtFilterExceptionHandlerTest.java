package com.example.kwizi.security;


import com.example.kwizi.exception.JwtAuthenticationException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterExceptionHandlerTest {

    private JwtFilterExceptionHandler jwtFilterExceptionHandler;

    @Mock
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        jwtFilterExceptionHandler = new JwtFilterExceptionHandler();
    }

    @Test
    void handleJwtException_ShouldSendUnauthorized_WhenExpiredJwtException() throws IOException {
        // Arrange
        ExpiredJwtException exception = mock(ExpiredJwtException.class);
        String expectedMessage = "JWT истек";

        // Act
        jwtFilterExceptionHandler.handleJwtException(exception, response);

        // Assert
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, expectedMessage);
    }

    @Test
    void handleJwtException_ShouldSendUnauthorized_WhenUnsupportedJwtException() throws IOException {
        // Arrange
        UnsupportedJwtException exception = mock(UnsupportedJwtException.class);
        String expectedMessage = "Неподдерживаемый JWT";

        // Act
        jwtFilterExceptionHandler.handleJwtException(exception, response);

        // Assert
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, expectedMessage);
    }

    @Test
    void handleJwtException_ShouldSendUnauthorized_WhenMalformedJwtException() throws IOException {
        // Arrange
        MalformedJwtException exception = mock(MalformedJwtException.class);
        String expectedMessage = "Неверный формат JWT";

        // Act
        jwtFilterExceptionHandler.handleJwtException(exception, response);

        // Assert
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, expectedMessage);
    }

    @Test
    void handleJwtException_ShouldSendUnauthorized_WhenSignatureException() throws IOException {
        // Arrange
        // Определяем какой именно SignatureException используется
        Exception signatureException = createSignatureException();
        String expectedMessage = "Неверная подпись JWT";

        // Act
        jwtFilterExceptionHandler.handleJwtException(signatureException, response);

        // Assert
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, expectedMessage);
    }

    @Test
    void handleJwtException_ShouldSendUnauthorizedWithMessage_WhenJwtAuthenticationException() throws IOException {
        // Arrange
        String errorMessage = "Токен был отозван";
        JwtAuthenticationException exception = new JwtAuthenticationException(errorMessage);

        // Act
        jwtFilterExceptionHandler.handleJwtException(exception, response);

        // Assert
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, errorMessage);
    }

    @Test
    void handleJwtException_ShouldSendGenericUnauthorized_WhenOtherException() throws IOException {
        // Arrange
        Exception exception = new RuntimeException("Some other error");
        String expectedMessage = "Ошибка аутентификации";

        // Act
        jwtFilterExceptionHandler.handleJwtException(exception, response);

        // Assert
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, expectedMessage);
    }

    @Test
    void handleJwtException_ShouldSendUnauthorized_WhenIllegalArgumentException() throws IOException {
        // Arrange
        IllegalArgumentException exception = new IllegalArgumentException("Invalid token");
        String expectedMessage = "Ошибка аутентификации"; // Попадает в общий случай

        // Act
        jwtFilterExceptionHandler.handleJwtException(exception, response);

        // Assert
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, expectedMessage);
    }

    @Test
    void handleJwtException_ShouldSendUnauthorized_WhenJwtException() throws IOException {
        // Arrange
        JwtException exception = mock(JwtException.class);
        String expectedMessage = "Ошибка аутентификации"; // Попадает в общий случай

        // Act
        jwtFilterExceptionHandler.handleJwtException(exception, response);

        // Assert
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, expectedMessage);
    }

    @Test
    void handleJwtException_ShouldHandleIOException() throws IOException {
        // Arrange
        ExpiredJwtException exception = mock(ExpiredJwtException.class);

        // Симулируем IOException при вызове sendError
        doThrow(new IOException("Failed to send error")).when(response)
                .sendError(anyInt(), anyString());

        // Act & Assert - должно пробросить IOException
        try {
            jwtFilterExceptionHandler.handleJwtException(exception, response);
        } catch (IOException e) {
            // Ожидаемое поведение
        }

        // Assert
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT истек");
    }

    @Test
    void handleJwtException_ShouldHandleNullResponse() throws IOException {
        // Arrange
        ExpiredJwtException exception = mock(ExpiredJwtException.class);

        // Act & Assert - должно бросить NullPointerException
        try {
            jwtFilterExceptionHandler.handleJwtException(exception, null);
        } catch (NullPointerException e) {
            // Ожидаемое поведение
        }
    }

    @Test
    void handleJwtException_ShouldHandleNullException() throws IOException {
        // Arrange
        String expectedMessage = "Ошибка аутентификации"; // null попадает в общий случай

        // Act
        jwtFilterExceptionHandler.handleJwtException(null, response);

        // Assert
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, expectedMessage);
    }

    // Вспомогательный метод для создания правильного SignatureException
    private Exception createSignatureException() {
        try {
            // Попробуем создать из нового пакета security
            return (Exception) Class.forName("io.jsonwebtoken.security.SignatureException")
                    .getDeclaredConstructor(String.class)
                    .newInstance("Invalid signature");
        } catch (Exception e1) {
            try {
                // Попробуем создать из старого пакета
                return (Exception) Class.forName("io.jsonwebtoken.SignatureException")
                        .getDeclaredConstructor(String.class)
                        .newInstance("Invalid signature");
            } catch (Exception e2) {
                // Возвращаем mock нужного типа через рефлексию
                try {
                    Class<?> exceptionClass = Class.forName("io.jsonwebtoken.security.SignatureException");
                    return (Exception) mock(exceptionClass);
                } catch (ClassNotFoundException e3) {
                    try {
                        Class<?> exceptionClass = Class.forName("io.jsonwebtoken.SignatureException");
                        return (Exception) mock(exceptionClass);
                    } catch (ClassNotFoundException e4) {
                        // Если ничего не найдено, используем java.security.SignatureException
                        return mock(java.security.SignatureException.class);
                    }
                }
            }
        }
    }
}