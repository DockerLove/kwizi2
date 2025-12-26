package com.example.kwizi.security;

import com.example.kwizi.exception.JwtAuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@DisplayName("JwtUtils тесты")
@ExtendWith(MockitoExtension.class)
class JwtUtilsTest {

    @Mock
    private JwtExceptionHandler jwtExceptionHandler;

    private JwtUtils jwtUtils;

    @Mock
    private HttpServletRequest request;

    @Mock
    private UserDetails userDetails;

    private static final String SECRET = "dGVzdFNlY3JldEtleTEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MA==";
    private static final String TEST_USERNAME = "testUser";
    private String validToken;

    @BeforeEach
    void setUp() {
        JwtExceptionHandler realHandler = new JwtExceptionHandler();
        jwtUtils = new JwtUtils(realHandler);
        ReflectionTestUtils.setField(jwtUtils, "secret", SECRET);
        jwtUtils.setSecret();
        validToken = jwtUtils.generateToken(TEST_USERNAME);
    }

    @Nested
    @DisplayName("Генерация токена")
    class GenerateTokenTests {

        @Test
        @DisplayName("✅ Возвращает валидный JWT при корректном имени пользователя")
        void generateToken_ShouldReturnToken_WhenUsernameIsValid() {
            String token = jwtUtils.generateToken(TEST_USERNAME);
            assertThat(token).isNotNull().isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        @DisplayName("❌ Выбрасывает исключение при пустом имени пользователя")
        void generateToken_ShouldThrowException_WhenUsernameIsInvalid(String username) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> jwtUtils.generateToken(username)
            );
            assertThat(exception.getMessage()).isEqualTo("Имя пользователя не может быть пустым");
        }

        @Test
        @DisplayName("✅ Генерирует разные токены для одного и того же пользователя")
        void generateToken_ShouldGenerateDifferentTokensForSameUser() {
            String token1 = jwtUtils.generateToken(TEST_USERNAME);
            String token2 = jwtUtils.generateToken(TEST_USERNAME);
            assertThat(token1).isNotNull();
            assertThat(token2).isNotNull();
            assertThat(token1).isNotEqualTo(token2);
        }
    }

    @Nested
    @DisplayName("Извлечение JTI")
    class ExtractJtiTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        @DisplayName("❌ Выбрасывает исключение при пустом токене")
        void extractJti_ShouldThrowException_WhenTokenIsInvalid(String token) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> jwtUtils.extractJti(token)
            );
            assertThat(exception.getMessage()).isEqualTo("Токен не может быть пустым");
        }
    }

    @Nested
    @DisplayName("Извлечение срока действия")
    class ExtractExpirationTests {

        @Test
        @DisplayName("❌ Выбрасывает исключение при невалидном токене")
        void extractExpiration_ShouldThrowException_WhenTokenIsInvalid() {
            String invalidToken = "invalid.token.here";
            JwtAuthenticationException exception = assertThrows(
                    JwtAuthenticationException.class,
                    () -> jwtUtils.extractExpiration(invalidToken)
            );
            assertThat(exception.getMessage()).contains("Не удалось извлечь срок действия токена");
        }
    }

    @Nested
    @DisplayName("Извлечение токена из запроса")
    class ExtractTokenTests {

        @Test
        @DisplayName("✅ Возвращает токен при валидном заголовке Authorization")
        void extractToken_ShouldReturnToken_WhenAuthorizationHeaderIsValid() {
            String token = "valid.token.here";
            when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
            String result = jwtUtils.extractToken(request);
            assertThat(result).isEqualTo(token);
        }

        @Test
        @DisplayName("❌ Выбрасывает исключение при null-запросе")
        void extractToken_ShouldThrowException_WhenRequestIsNull() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> jwtUtils.extractToken(null)
            );
            assertThat(exception.getMessage()).isEqualTo("HttpServletRequest не может быть null");
        }

        @Test
        @DisplayName("❌ Выбрасывает исключение при отсутствии заголовка Authorization")
        void extractToken_ShouldThrowException_WhenAuthorizationHeaderIsNull() {
            when(request.getHeader("Authorization")).thenReturn(null);
            JwtAuthenticationException exception = assertThrows(
                    JwtAuthenticationException.class,
                    () -> jwtUtils.extractToken(request)
            );
            assertThat(exception.getMessage()).contains("Отсутствует или невалидный заголовок Authorization");
        }

        @Test
        @DisplayName("❌ Выбрасывает исключение при неверном префиксе заголовка")
        void extractToken_ShouldThrowException_WhenAuthorizationHeaderDoesNotStartWithBearer() {
            when(request.getHeader("Authorization")).thenReturn("Basic someToken");
            JwtAuthenticationException exception = assertThrows(
                    JwtAuthenticationException.class,
                    () -> jwtUtils.extractToken(request)
            );
            assertThat(exception.getMessage()).contains("Отсутствует или невалидный заголовок Authorization");
        }

        @Test
        @DisplayName("❌ Выбрасывает исключение при пустом токене после Bearer")
        void extractToken_ShouldThrowException_WhenTokenIsEmptyAfterBearer() {
            when(request.getHeader("Authorization")).thenReturn("Bearer ");
            JwtAuthenticationException exception = assertThrows(
                    JwtAuthenticationException.class,
                    () -> jwtUtils.extractToken(request)
            );
            assertThat(exception.getMessage()).contains("Токен не может быть пустым");
        }

        @Test
        @DisplayName("✅ Обрезает пробелы вокруг токена")
        void extractToken_ShouldTrimToken() {
            String token = "valid.token.here";
            when(request.getHeader("Authorization")).thenReturn("Bearer  " + token + "  ");
            String result = jwtUtils.extractToken(request);
            assertThat(result).isEqualTo(token);
        }
    }

    @Nested
    @DisplayName("Валидация токена")
    class ValidateTokenTests {

        @Test
        @DisplayName("❌ Возвращает false при null-токене")
        void validateToken_ShouldReturnFalse_WhenTokenIsNull() {
            boolean result = jwtUtils.validateToken(null, userDetails);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("❌ Возвращает false при null-UserDetails")
        void validateToken_ShouldReturnFalse_WhenUserDetailsIsNull() {
            boolean result = jwtUtils.validateToken(validToken, null);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("✅ Возвращает true при валидном токене и совпадающем пользователе")
        void validateToken_ShouldReturnTrue_WhenTokenIsValidForUser() {
            when(userDetails.getUsername()).thenReturn(TEST_USERNAME);
            boolean result = jwtUtils.validateToken(validToken, userDetails);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("❌ Возвращает false при несовпадающем пользователе")
        void validateToken_ShouldReturnFalse_WhenTokenIsForDifferentUser() {
            String token = jwtUtils.generateToken(TEST_USERNAME);
            when(userDetails.getUsername()).thenReturn("differentUser");
            boolean result = jwtUtils.validateToken(token, userDetails);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("❌ Выбрасывает исключение при невалидном формате токена")
        void validateToken_ShouldThrowException_WhenTokenIsInvalid() {
            String invalidToken = "invalid.token";
            when(userDetails.getUsername()).thenReturn(TEST_USERNAME);
            assertThrows(
                    JwtAuthenticationException.class,
                    () -> jwtUtils.validateToken(invalidToken, userDetails)
            );
        }

        @Test
        @DisplayName("❌ Возвращает false для просроченного токена")
        void validateToken_ShouldReturnFalse_ForExpiredToken() {
            JwtUtils shortLivedJwtUtils = createShortLivedJwtUtils();
            String token = shortLivedJwtUtils.generateToken(TEST_USERNAME);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            when(userDetails.getUsername()).thenReturn(TEST_USERNAME);
            boolean result = shortLivedJwtUtils.validateToken(token, userDetails);
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Извлечение имени пользователя")
    class GetUsernameFromTokenTests {

        @Test
        @DisplayName("✅ Возвращает имя пользователя из валидного токена")
        void getUsernameFromToken_ShouldReturnUsername_WhenTokenIsValid() {
            String token = jwtUtils.generateToken(TEST_USERNAME);
            String username = jwtUtils.getUsernameFromToken(token);
            assertThat(username).isEqualTo(TEST_USERNAME);
        }
    }

    @Nested
    @DisplayName("Проверка срока действия")
    class IsTokenExpiredTests {

        @Test
        @DisplayName("✅ Возвращает false для невышедшего токена")
        void isTokenExpired_ShouldReturnFalse_WhenTokenIsNotExpired() {
            boolean result = jwtUtils.isTokenExpired(validToken);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("❌ Выбрасывает исключение при невалидном токене")
        void isTokenExpired_ShouldThrowException_WhenTokenIsInvalid() {
            String invalidToken = "invalid.token";
            assertThrows(
                    Exception.class,
                    () -> jwtUtils.isTokenExpired(invalidToken)
            );
        }
    }

    @Nested
    @DisplayName("Инициализация секретного ключа")
    class SetSecretTests {

        @Test
        @DisplayName("✅ Инициализирует ключ при валидном секрете")
        void setSecret_ShouldInitializeKey_WhenSecretIsValid() {
            String token = jwtUtils.generateToken(TEST_USERNAME);
            assertThat(token).isNotNull();
        }

        @Test
        @DisplayName("❌ Выбрасывает исключение при невалидном Base64 секрете")
        void setSecret_ShouldThrowException_WhenSecretIsInvalid() {
            JwtUtils jwtUtilsWithInvalidSecret = new JwtUtils(jwtExceptionHandler);
            ReflectionTestUtils.setField(jwtUtilsWithInvalidSecret, "secret", "invalid-base64");
            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> jwtUtilsWithInvalidSecret.setSecret()
            );
            assertThat(exception.getMessage()).contains("Не удалось инициализировать секретный ключ JWT");
        }

        @Test
        @DisplayName("❌ Выбрасывает исключение при null-секрете")
        void setSecret_ShouldThrowException_WhenSecretIsNull() {
            JwtUtils jwtUtilsWithNullSecret = new JwtUtils(jwtExceptionHandler);
            ReflectionTestUtils.setField(jwtUtilsWithNullSecret, "secret", null);
            assertThrows(
                    Exception.class,
                    () -> jwtUtilsWithNullSecret.setSecret()
            );
        }
    }

    private JwtUtils createShortLivedJwtUtils() {
        JwtUtils shortLivedJwtUtils = new JwtUtils(jwtExceptionHandler);
        ReflectionTestUtils.setField(shortLivedJwtUtils, "secret", SECRET);
        shortLivedJwtUtils.setSecret();
        return shortLivedJwtUtils;
    }
}