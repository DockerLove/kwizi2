package com.example.kwizi.security;

import com.example.kwizi.exception.JwtAuthenticationException;
import com.example.kwizi.repository.RevokedTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("JwtUtils тесты")
class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @Mock
    private UserDetails userDetails;

    @Mock
    private RevokedTokenRepository revokedTokenRepository;
    private static final String SECRET = "dGVzdFNlY3JldEtleTEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MA==";
    private static final String TEST_USERNAME = "testUser";
    private static final Long TEST_USER_ID = 123L;
    private String validTokenWithUserId;

    @BeforeEach
    void setUp() {
        JwtExceptionHandler realHandler = new JwtExceptionHandler();
        jwtUtils = new JwtUtils(realHandler,revokedTokenRepository);
        ReflectionTestUtils.setField(jwtUtils, "secret", SECRET);
        jwtUtils.setSecret();
        validTokenWithUserId = jwtUtils.generateToken(TEST_USERNAME, TEST_USER_ID);
    }

    @Nested
    @DisplayName("Генерация токена")
    class GenerateTokenTests {

        @Test
        @DisplayName("✅ Возвращает валидный JWT при корректном имени пользователя и ID")
        void generateToken_ShouldReturnToken_WhenUsernameAndIdAreValid() {
            String token = jwtUtils.generateToken(TEST_USERNAME, TEST_USER_ID);
            assertThat(token).isNotNull().isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        @DisplayName("❌ Выбрасывает исключение при пустом имени пользователя (новая сигнатура)")
        void generateToken_ShouldThrowException_WhenUsernameIsInvalid_NewSignature(String username) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> jwtUtils.generateToken(username, TEST_USER_ID)
            );
            assertThat(exception.getMessage()).isEqualTo("Имя пользователя не может быть пустым");
        }

        @Test
        @DisplayName("❌ Выбрасывает исключение при недопустимом ID пользователя (новая сигнатура)")
        void generateToken_ShouldThrowException_WhenUserIdIsInvalid() {
            assertAll(
                    () -> assertThatThrownBy(() -> jwtUtils.generateToken(TEST_USERNAME, null))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("ID пользователя должен быть положительным числом"),
                    () -> assertThatThrownBy(() -> jwtUtils.generateToken(TEST_USERNAME, 0L))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("ID пользователя должен быть положительным числом"),
                    () -> assertThatThrownBy(() -> jwtUtils.generateToken(TEST_USERNAME, -1L))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessage("ID пользователя должен быть положительным числом")
            );
        }

        @Test
        @DisplayName("✅ Генерирует разные токены для одного и того же пользователя и ID")
        void generateToken_ShouldGenerateDifferentTokensForSameUserAndId() {
            String token1 = jwtUtils.generateToken(TEST_USERNAME, TEST_USER_ID);
            String token2 = jwtUtils.generateToken(TEST_USERNAME, TEST_USER_ID);
            assertThat(token1).isNotNull();
            assertThat(token2).isNotNull();
            assertThat(token1).isNotEqualTo(token2);
        }

        // === Сохраняем старые тесты для обратной совместимости (если метод ещё существует) ===

        @Test
        @DisplayName("✅ Возвращает валидный JWT при корректном имени пользователя (старая сигнатура)")
        void generateToken_ShouldReturnToken_WhenUsernameIsValid_OldSignature() {
            String token = jwtUtils.generateToken(TEST_USERNAME,TEST_USER_ID);
            assertThat(token).isNotNull().isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        @DisplayName("❌ Выбрасывает исключение при пустом имени пользователя (старая сигнатура)")
        void generateToken_ShouldThrowException_WhenUsernameIsInvalid_OldSignature(String username) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> jwtUtils.generateToken(username,TEST_USER_ID)
            );
            assertThat(exception.getMessage()).isEqualTo("Имя пользователя не может быть пустым");
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

        @Test
        @DisplayName("✅ Извлекает JTI из валидного токена с userId")
        void extractJti_ShouldReturnJti_WhenTokenIsValid() {
            String jti = jwtUtils.extractJti(validTokenWithUserId);
            assertThat(jti).isNotNull().isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Извлечение userId")
    class GetUserIdFromTokenTests {

        @Test
        @DisplayName("✅ Возвращает userId из валидного токена")
        void getUserIdFromToken_ShouldReturnUserId_WhenTokenIsValid() {
            Long userId = jwtUtils.getUserIdFromToken(validTokenWithUserId);
            assertThat(userId).isEqualTo(TEST_USER_ID);
        }

        @Test
        @DisplayName("❌ Выбрасывает исключение при невалидном токене")
        void getUserIdFromToken_ShouldThrowException_WhenTokenIsInvalid() {
            assertThatThrownBy(() -> jwtUtils.getUserIdFromToken("invalid.token.here"))
                    .isInstanceOf(JwtAuthenticationException.class);
        }
    }

    @Nested
    @DisplayName("Извлечение срока действия")
    class ExtractExpirationTests {

        @Test
        @DisplayName("❌ Выбрасывает исключение при невалидном токене")
        void extractExpiration_ShouldThrowException_WhenTokenIsInvalid() {
            String invalidToken = "invalid.token.here";
            assertThatThrownBy(() -> jwtUtils.extractExpiration(invalidToken))
                    .isInstanceOf(JwtAuthenticationException.class)
                    .hasMessageContaining("Не удалось извлечь срок действия токена");
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
            boolean result = jwtUtils.validateToken(validTokenWithUserId, null);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("✅ Возвращает true при валидном токене и совпадающем пользователе")
        void validateToken_ShouldReturnTrue_WhenTokenIsValidForUser() {
            UserDetails userDetails = mock(UserDetails.class);
            when(userDetails.getUsername()).thenReturn(TEST_USERNAME);
            boolean result = jwtUtils.validateToken(validTokenWithUserId, userDetails);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("❌ Возвращает false при несовпадающем пользователе")
        void validateToken_ShouldReturnFalse_WhenTokenIsForDifferentUser() {
            UserDetails userDetails = mock(UserDetails.class);
            when(userDetails.getUsername()).thenReturn("differentUser");
            boolean result = jwtUtils.validateToken(validTokenWithUserId, userDetails);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("❌ Выбрасывает исключение при невалидном формате токена")
        void validateToken_ShouldThrowException_WhenTokenIsInvalid() {
            UserDetails userDetails = mock(UserDetails.class);
            when(userDetails.getUsername()).thenReturn(TEST_USERNAME);
            assertThatThrownBy(() -> jwtUtils.validateToken("invalid.token", userDetails))
                    .isInstanceOf(JwtAuthenticationException.class);
        }

    }
    @Nested
    @DisplayName("Извлечение имени пользователя")
    class GetUsernameFromTokenTests {

        @Test
        @DisplayName("✅ Возвращает имя пользователя из валидного токена с userId")
        void getUsernameFromToken_ShouldReturnUsername_WhenTokenIsValid() {
            String username = jwtUtils.getUsernameFromToken(validTokenWithUserId);
            assertThat(username).isEqualTo(TEST_USERNAME);
        }

    }
    @Nested
    @DisplayName("Проверка срока действия")
    class IsTokenExpiredTests {

        @Test
        @DisplayName("✅ Возвращает false для невышедшего токена")
        void isTokenExpired_ShouldReturnFalse_WhenTokenIsNotExpired() {
            boolean result = jwtUtils.isTokenExpired(validTokenWithUserId);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("❌ Выбрасывает исключение при невалидном токене")
        void isTokenExpired_ShouldThrowException_WhenTokenIsInvalid() {
            assertThatThrownBy(() -> jwtUtils.isTokenExpired("invalid.token"))
                    .isInstanceOf(JwtAuthenticationException.class);
        }

    }
    @Nested
    @DisplayName("Инициализация секретного ключа")
    class SetSecretTests {

        @Test
        @DisplayName("✅ Инициализирует ключ при валидном секрете")
        void setSecret_ShouldInitializeKey_WhenSecretIsValid() {
            String token = jwtUtils.generateToken(TEST_USERNAME, TEST_USER_ID);
            assertThat(token).isNotNull();
        }

        @Test
        @DisplayName("❌ Выбрасывает исключение при невалидном Base64 секрете")
        void setSecret_ShouldThrowException_WhenSecretIsInvalid() {
            JwtUtils jwtUtilsWithInvalidSecret = new JwtUtils(new JwtExceptionHandler(),revokedTokenRepository);
            ReflectionTestUtils.setField(jwtUtilsWithInvalidSecret, "secret", "invalid-base64");
            assertThatThrownBy(() -> jwtUtilsWithInvalidSecret.setSecret())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Не удалось инициализировать секретный ключ JWT");
        }

        @Test
        @DisplayName("❌ Выбрасывает исключение при null-секрете")
        void setSecret_ShouldThrowException_WhenSecretIsNull() {
            JwtUtils jwtUtilsWithNullSecret = new JwtUtils(new JwtExceptionHandler(),revokedTokenRepository);
            ReflectionTestUtils.setField(jwtUtilsWithNullSecret, "secret", null);
            assertThatThrownBy(() -> jwtUtilsWithNullSecret.setSecret())
                    .isInstanceOf(Exception.class);
        }

    }
    @Nested
    @DisplayName("Извлечение токена из запроса")
    class ExtractTokenTests {

        @Test
        @DisplayName("✅ Возвращает токен при валидном заголовке Authorization")
        void extractToken_ShouldReturnToken_WhenAuthorizationHeaderIsValid() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            String token = "valid.token.here";
            request.addHeader("Authorization", "Bearer " + token);
            String result = jwtUtils.extractToken(request);
            assertThat(result).isEqualTo(token);
        }

        @Test
        @DisplayName("❌ Выбрасывает исключение при null-запросе")
        void extractToken_ShouldThrowException_WhenRequestIsNull() {
            assertThatThrownBy(() -> jwtUtils.extractToken(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("HttpServletRequest не может быть null");
        }

        @Test
        @DisplayName("❌ Выбрасывает исключение при отсутствии заголовка Authorization")
        void extractToken_ShouldThrowException_WhenAuthorizationHeaderIsNull() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            assertThatThrownBy(() -> jwtUtils.extractToken(request))
                    .isInstanceOf(JwtAuthenticationException.class)
                    .hasMessageContaining("Отсутствует или невалидный заголовок Authorization");
        }

        @Test
        @DisplayName("❌ Выбрасывает исключение при неверном префиксе заголовка")
        void extractToken_ShouldThrowException_WhenAuthorizationHeaderDoesNotStartWithBearer() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Basic someToken");
            assertThatThrownBy(() -> jwtUtils.extractToken(request))
                    .isInstanceOf(JwtAuthenticationException.class)
                    .hasMessageContaining("Отсутствует или невалидный заголовок Authorization");
        }

        @Test
        @DisplayName("❌ Выбрасывает исключение при пустом токене после Bearer")
        void extractToken_ShouldThrowException_WhenTokenIsEmptyAfterBearer() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer ");
            assertThatThrownBy(() -> jwtUtils.extractToken(request))
                    .isInstanceOf(JwtAuthenticationException.class)
                    .hasMessageContaining("Токен не может быть пустым");
        }

        @Test
        @DisplayName("✅ Обрезает пробелы вокруг токена")
        void extractToken_ShouldTrimToken() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            String token = "valid.token.here";
            request.addHeader("Authorization", "Bearer  " + token + "  ");
            String result = jwtUtils.extractToken(request);
            assertThat(result).isEqualTo(token);
        }
    }

    private JwtUtils createShortLivedJwtUtils() {
        JwtUtils shortLivedJwtUtils = new JwtUtils(new JwtExceptionHandler(),revokedTokenRepository);
        ReflectionTestUtils.setField(shortLivedJwtUtils, "secret", SECRET);
        shortLivedJwtUtils.setSecret();
        return shortLivedJwtUtils;
    }
}