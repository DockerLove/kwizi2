package com.example.kwizi.security;

import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
@DisplayName("JwtEmailVerify тесты")
@ExtendWith(MockitoExtension.class)
public class JwtEmailVerifyTest {
    private JwtEmailVerify jwtEmailVerify;

    private final String validSecret = "testsecrettestsecrettestsecrettestsecret";
    private final long validExpiration = 3600000;
    private Key testKey;

    @BeforeEach
    void setUp() {
        jwtEmailVerify = new JwtEmailVerify();
        testKey = Keys.hmacShaKeyFor(validSecret.getBytes());
        ReflectionTestUtils.setField(jwtEmailVerify, "secret", validSecret);
        ReflectionTestUtils.setField(jwtEmailVerify, "verificationTokenExpiration", validExpiration);
        jwtEmailVerify.setSecret();
        ReflectionTestUtils.setField(jwtEmailVerify, "key", testKey);
    }

    @Nested
    @DisplayName("Генерация токена подтверждения")
    class GenerateVerificationTokenTests {

        @Test
        @DisplayName("Генерирует непустой токен при валидном ID пользователя")
        void generateVerificationToken_ShouldGenerateValidToken() {
            Long userId = 123L;
            String token = jwtEmailVerify.generateVerificationToken(userId);
            assertThat(token).isNotNull().isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Извлечение ID пользователя")
    class GetUserIdFromTokenTests {

        @Test
        @DisplayName("Возвращает корректный ID пользователя из валидного токена")
        void getUserIdFromToken_ShouldReturnUserId_WhenTokenIsValid() {
            Long userId = 123L;
            String token = jwtEmailVerify.generateVerificationToken(userId);
            String extractedUserId = jwtEmailVerify.getUserIdFromToken(token);
            assertThat(extractedUserId).isEqualTo(String.valueOf(userId));
        }

        @Test
        @DisplayName("Возвращает null при невалидном токене")
        void getUserIdFromToken_ShouldReturnNull_WhenTokenIsInvalid() {
            String invalidToken = "invalidToken";
            String extractedUserId = jwtEmailVerify.getUserIdFromToken(invalidToken);
            assertThat(extractedUserId).isNull();
        }
    }

    @Nested
    @DisplayName("Проверка срока действия токена")
    class IsTokenExpiredTests {

        @Test
        @DisplayName("Возвращает false для невышедшего токена")
        void isTokenExpired_ShouldReturnFalse_WhenTokenIsNotExpired() {
            Long userId = 123L;
            String token = jwtEmailVerify.generateVerificationToken(userId);
            boolean isExpired = jwtEmailVerify.isTokenExpired(token);
            assertThat(isExpired).isFalse();
        }

        @Test
        @DisplayName("Возвращает true для просроченного токена")
        void isTokenExpired_ShouldReturnTrue_WhenTokenIsExpired() throws InterruptedException {
            JwtEmailVerify jwtEmailVerifyExpired = new JwtEmailVerify();
            ReflectionTestUtils.setField(jwtEmailVerifyExpired, "secret", validSecret);
            ReflectionTestUtils.setField(jwtEmailVerifyExpired, "verificationTokenExpiration", 1);
            jwtEmailVerifyExpired.setSecret();

            Long userId = 123L;
            String token = jwtEmailVerifyExpired.generateVerificationToken(userId);
            Thread.sleep(2);
            boolean isExpired = jwtEmailVerifyExpired.isTokenExpired(token);
            assertThat(isExpired).isTrue();
        }

        @Test
        @DisplayName("Возвращает true при невалидном токене")
        void isTokenExpired_ShouldReturnTrue_WhenTokenIsInvalid() {
            String invalidToken = "invalidToken";
            boolean isExpired = jwtEmailVerify.isTokenExpired(invalidToken);
            assertThat(isExpired).isTrue();
        }
    }

    @Nested
    @DisplayName("Инициализация секретного ключа")
    class SetSecretTests {

        @Test
        @DisplayName("Не выбрасывает исключение при валидном секрете")
        void setSecret_ShouldNotThrowException_WhenSecretIsValid() {
            assertThatNoException().isThrownBy(() -> {
            });
        }
    }
}