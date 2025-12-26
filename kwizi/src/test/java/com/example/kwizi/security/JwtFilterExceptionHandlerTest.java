package com.example.kwizi.security;


import com.example.kwizi.exception.JwtAuthenticationException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
@DisplayName("JwtFilterExceptionHandler тесты")
@ExtendWith(MockitoExtension.class)
class JwtFilterExceptionHandlerTest {
    private JwtFilterExceptionHandler jwtFilterExceptionHandler;

    @Mock
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        jwtFilterExceptionHandler = new JwtFilterExceptionHandler();
    }

    @Nested
    @DisplayName("Обработка специфичных исключений JWT")
    class SpecificJwtExceptionHandling {

        @Test
        @DisplayName("✅ Отправляет 401 при ExpiredJwtException")
        void handleJwtException_ShouldSendUnauthorized_WhenExpiredJwtException() throws IOException {
            ExpiredJwtException exception = mock(ExpiredJwtException.class);
            jwtFilterExceptionHandler.handleJwtException(exception, response);
            verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT истек");
        }

        @Test
        @DisplayName("✅ Отправляет 401 при UnsupportedJwtException")
        void handleJwtException_ShouldSendUnauthorized_WhenUnsupportedJwtException() throws IOException {
            UnsupportedJwtException exception = mock(UnsupportedJwtException.class);
            jwtFilterExceptionHandler.handleJwtException(exception, response);
            verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Неподдерживаемый JWT");
        }

        @Test
        @DisplayName("✅ Отправляет 401 при MalformedJwtException")
        void handleJwtException_ShouldSendUnauthorized_WhenMalformedJwtException() throws IOException {
            MalformedJwtException exception = mock(MalformedJwtException.class);
            jwtFilterExceptionHandler.handleJwtException(exception, response);
            verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Неверный формат JWT");
        }

        @Test
        @DisplayName("✅ Отправляет 401 при SignatureException")
        void handleJwtException_ShouldSendUnauthorized_WhenSignatureException() throws IOException {
            Exception signatureException = createSignatureException();
            jwtFilterExceptionHandler.handleJwtException(signatureException, response);
            verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Неверная подпись JWT");
        }

        @Test
        @DisplayName("✅ Отправляет 401 с сообщением при JwtAuthenticationException")
        void handleJwtException_ShouldSendUnauthorizedWithMessage_WhenJwtAuthenticationException() throws IOException {
            String errorMessage = "Токен был отозван";
            JwtAuthenticationException exception = new JwtAuthenticationException(errorMessage);
            jwtFilterExceptionHandler.handleJwtException(exception, response);
            verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, errorMessage);
        }
    }

    @Nested
    @DisplayName("Обработка общих и неизвестных исключений")
    class GenericExceptionHandling {

        @ParameterizedTest
        @ValueSource(classes = {RuntimeException.class, IllegalArgumentException.class, JwtException.class})
        @DisplayName("✅ Отправляет 401 с общим сообщением для неизвестных исключений")
        void handleJwtException_ShouldSendGenericUnauthorized_WhenOtherException(Class<? extends Exception> exceptionClass) throws IOException {
            Exception exception = mock(exceptionClass);
            jwtFilterExceptionHandler.handleJwtException(exception, response);
            verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Ошибка аутентификации");
        }

        @Test
        @DisplayName("✅ Отправляет 401 с общим сообщением при null-исключении")
        void handleJwtException_ShouldHandleNullException() throws IOException {
            jwtFilterExceptionHandler.handleJwtException(null, response);
            verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Ошибка аутентификации");
        }
    }

    @Nested
    @DisplayName("Крайние случаи")
    class EdgeCases {

        @Test
        @DisplayName("✅ Пробрасывает IOException при ошибке отправки ответа")
        void handleJwtException_ShouldHandleIOException() throws IOException {
            ExpiredJwtException exception = mock(ExpiredJwtException.class);
            doThrow(new IOException("Failed to send error")).when(response)
                    .sendError(anyInt(), anyString());

            IOException thrown = assertThrows(IOException.class, () -> {
                jwtFilterExceptionHandler.handleJwtException(exception, response);
            });

            assertThat(thrown.getMessage()).isEqualTo("Failed to send error");
            verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT истек");
        }

        @Test
        @DisplayName("✅ Выбрасывает NullPointerException при null-ответе")
        void handleJwtException_ShouldHandleNullResponse() {
            ExpiredJwtException exception = mock(ExpiredJwtException.class);
            assertThrows(NullPointerException.class, () -> {
                jwtFilterExceptionHandler.handleJwtException(exception, null);
            });
        }
    }

    private Exception createSignatureException() {
        try {
            return (Exception) Class.forName("io.jsonwebtoken.security.SignatureException")
                    .getDeclaredConstructor(String.class)
                    .newInstance("Invalid signature");
        } catch (Exception e1) {
            try {
                return (Exception) Class.forName("io.jsonwebtoken.SignatureException")
                        .getDeclaredConstructor(String.class)
                        .newInstance("Invalid signature");
            } catch (Exception e2) {
                try {
                    Class<?> exceptionClass = Class.forName("io.jsonwebtoken.security.SignatureException");
                    return (Exception) mock(exceptionClass);
                } catch (ClassNotFoundException e3) {
                    try {
                        Class<?> exceptionClass = Class.forName("io.jsonwebtoken.SignatureException");
                        return (Exception) mock(exceptionClass);
                    } catch (ClassNotFoundException e4) {
                        return mock(java.security.SignatureException.class);
                    }
                }
            }
        }
    }
}