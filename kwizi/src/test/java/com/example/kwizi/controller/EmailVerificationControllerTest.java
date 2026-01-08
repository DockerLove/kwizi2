package com.example.kwizi.controller;

import com.example.kwizi.service.AuthenticationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@DisplayName("EmailVerificationController тесты")
@ExtendWith(MockitoExtension.class)
class EmailVerificationControllerTest {

    @Mock
    private AuthenticationService authService;

    @InjectMocks
    private EmailVerificationController controller;

    @Nested
    @DisplayName("Основные сценарии")
    class MainScenarios {

        @Test
        @DisplayName("✅ Успешное подтверждение email")
        void success() {
            String token = "valid-token";

            doNothing().when(authService).verifyEmail(token);

            var response = controller.verifyEmail(token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_HTML);

            String body = response.getBody();
            assertThat(body)
                    .contains("Email успешно подтвержден!")
                    .contains("✓")
                    .contains("<!DOCTYPE html>")
                    .contains("</html>");
        }
    }

    @Nested
    @DisplayName("Параметризованные тесты для разных токенов")
    class ParameterizedTokenTests {

        @ParameterizedTest
        @ValueSource(strings = {"short", "very-long-token-12345", "with-special_chars"})
        @DisplayName("Разные форматы валидных токенов")
        void validTokenFormats(String token) {
            assertThat(controller.verifyEmail(token).getStatusCode().value())
                    .isEqualTo(200);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\n\t"})
        @DisplayName("Пустые и null токены")
        void invalidTokens(String token) {
            doThrow(new IllegalArgumentException("Invalid token"))
                    .when(authService).verifyEmail(token);

            assertThat(controller.verifyEmail(token).getStatusCode().value())
                    .isEqualTo(400);
        }
    }
}