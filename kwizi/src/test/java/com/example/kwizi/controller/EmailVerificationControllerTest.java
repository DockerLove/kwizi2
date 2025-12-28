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
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
//todo вот пример хорошего unit теста для класса надо будет переписать все остальные классы также +
// для контроллеров только надо написать интеграционные тесты это тоже отдельный клас но под инт тесты +
// также надо уточнить то нужно использовать AssertJ для тестов
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
            // given
            String token = "valid-token";

            // when
            var response = controller.verifyEmail(token);

            // then
            assertThat(response)
                    .extracting(r -> r.getStatusCode().value(), ResponseEntity::getBody)
                    .containsExactly(200, "Email успешно подтвержден! Можете закрыть эту страницу.");
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