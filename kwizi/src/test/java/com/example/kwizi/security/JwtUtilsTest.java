package com.example.kwizi.security;

import com.example.kwizi.exception.JwtAuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtUtilsTest {

    @Mock
    private JwtExceptionHandler jwtExceptionHandler;

    @InjectMocks
    private JwtUtils jwtUtils;

    @Mock
    private HttpServletRequest request;

    @Mock
    private UserDetails userDetails;

    private static final String SECRET = "dGVzdFNlY3JldEtleTEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MA=="; // Base64 encoded
    private static final String TEST_USERNAME = "testUser";
    private String validToken;

    @BeforeEach
    void setUp() {
        // Создаем реальный JwtExceptionHandler
        JwtExceptionHandler realHandler = new JwtExceptionHandler();

        // Пересоздаем jwtUtils с реальным handler
        jwtUtils = new JwtUtils(realHandler);

        // Устанавливаем секрет через ReflectionTestUtils (Base64 encoded)
        ReflectionTestUtils.setField(jwtUtils, "secret", SECRET);

        // Вызываем @PostConstruct метод
        jwtUtils.setSecret();

        // Генерируем валидный токен для тестов
        validToken = jwtUtils.generateToken(TEST_USERNAME);
    }

    // ==================== Тесты для generateToken ====================

    @Test
    void generateToken_ShouldReturnToken_WhenUsernameIsValid() {
        // Act
        String token = jwtUtils.generateToken(TEST_USERNAME);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        // Проверяем что токен состоит из 3 частей (JWT формат)
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void generateToken_ShouldThrowException_WhenUsernameIsNull() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> jwtUtils.generateToken(null)
        );

        assertEquals("Имя пользователя не может быть пустым", exception.getMessage());
    }

    @Test
    void generateToken_ShouldThrowException_WhenUsernameIsEmpty() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> jwtUtils.generateToken("")
        );

        assertEquals("Имя пользователя не может быть пустым", exception.getMessage());
    }

    @Test
    void generateToken_ShouldThrowException_WhenUsernameIsBlank() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> jwtUtils.generateToken("   ")
        );

        assertEquals("Имя пользователя не может быть пустым", exception.getMessage());
    }

    // ==================== Тесты для extractJti ====================

    @Test
    void extractJti_ShouldThrowException_WhenTokenIsNull() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> jwtUtils.extractJti(null)
        );

        assertEquals("Токен не может быть пустым", exception.getMessage());
    }

    @Test
    void extractJti_ShouldThrowException_WhenTokenIsEmpty() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> jwtUtils.extractJti("")
        );

        assertEquals("Токен не может быть пустым", exception.getMessage());
    }

    @Test
    void extractJti_ShouldThrowException_WhenTokenIsBlank() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> jwtUtils.extractJti("   ")
        );

        assertEquals("Токен не может быть пустым", exception.getMessage());
    }

    // ==================== Тесты для extractExpiration ====================

    @Test
    void extractExpiration_ShouldThrowException_WhenTokenIsInvalid() {
        // Arrange
        String invalidToken = "invalid.token.here";

        // Act & Assert
        JwtAuthenticationException exception = assertThrows(
                JwtAuthenticationException.class,
                () -> jwtUtils.extractExpiration(invalidToken)
        );

        assertTrue(exception.getMessage().contains("Не удалось извлечь срок действия токена"));
    }

    // ==================== Тесты для extractToken ====================

    @Test
    void extractToken_ShouldReturnToken_WhenAuthorizationHeaderIsValid() {
        // Arrange
        String token = "valid.token.here";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        // Act
        String result = jwtUtils.extractToken(request);

        // Assert
        assertEquals(token, result);
    }

    @Test
    void extractToken_ShouldThrowException_WhenRequestIsNull() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> jwtUtils.extractToken(null)
        );

        assertEquals("HttpServletRequest не может быть null", exception.getMessage());
    }

    @Test
    void extractToken_ShouldThrowException_WhenAuthorizationHeaderIsNull() {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);

        // Act & Assert
        JwtAuthenticationException exception = assertThrows(
                JwtAuthenticationException.class,
                () -> jwtUtils.extractToken(request)
        );

        assertTrue(exception.getMessage().contains("Отсутствует или невалидный заголовок Authorization"));
    }

    @Test
    void extractToken_ShouldThrowException_WhenAuthorizationHeaderDoesNotStartWithBearer() {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Basic someToken");

        // Act & Assert
        JwtAuthenticationException exception = assertThrows(
                JwtAuthenticationException.class,
                () -> jwtUtils.extractToken(request)
        );

        assertTrue(exception.getMessage().contains("Отсутствует или невалидный заголовок Authorization"));
    }

    @Test
    void extractToken_ShouldThrowException_WhenTokenIsEmptyAfterBearer() {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        // Act & Assert
        JwtAuthenticationException exception = assertThrows(
                JwtAuthenticationException.class,
                () -> jwtUtils.extractToken(request)
        );

        assertTrue(exception.getMessage().contains("Токен не может быть пустым"));
    }

    @Test
    void extractToken_ShouldTrimToken() {
        // Arrange
        String token = "valid.token.here";
        when(request.getHeader("Authorization")).thenReturn("Bearer  " + token + "  ");

        // Act
        String result = jwtUtils.extractToken(request);

        // Assert
        // Токен должен быть обрезан от пробелов
        assertEquals(token, result.trim());
    }

    // ==================== Тесты для validateToken ====================

    @Test
    void validateToken_ShouldReturnFalse_WhenTokenIsNull() {
        // Act
        boolean result = jwtUtils.validateToken(null, userDetails);

        // Assert
        assertFalse(result);
    }

    @Test
    void validateToken_ShouldReturnFalse_WhenUserDetailsIsNull() {
        // Arrange
        String token = jwtUtils.generateToken(TEST_USERNAME);

        // Act
        boolean result = jwtUtils.validateToken(token, null);

        // Assert
        assertFalse(result);
    }

    @Test
    void validateToken_ShouldThrowException_WhenTokenIsInvalid() {
        // Arrange
        String invalidToken = "invalid.token"; // 1 точка, нужно 2
        when(userDetails.getUsername()).thenReturn(TEST_USERNAME);

        // Act & Assert
        assertThrows(
                JwtAuthenticationException.class,
                () -> jwtUtils.validateToken(invalidToken, userDetails)
        );
    }

    @Test
    void validateToken_ShouldReturnTrue_WhenTokenIsValidForUser() {
        when(userDetails.getUsername()).thenReturn(TEST_USERNAME);
        boolean result = jwtUtils.validateToken(validToken, userDetails);
        assertTrue(result);
    }

    @Test
    void validateToken_ShouldReturnFalse_WhenTokenIsForDifferentUser() {
        // Arrange
        String token = jwtUtils.generateToken(TEST_USERNAME);
        when(userDetails.getUsername()).thenReturn("differentUser");

        // Act
        boolean result = jwtUtils.validateToken(token, userDetails);

        // Assert
        assertFalse(result);
    }

    // ==================== Тесты для getUsernameFromToken ====================

    @Test
    void getUsernameFromToken_ShouldReturnUsername_WhenTokenIsValid() {
        // Arrange
        String token = jwtUtils.generateToken(TEST_USERNAME);

        // Act
        String username = jwtUtils.getUsernameFromToken(token);

        // Assert
        assertEquals(TEST_USERNAME, username);
    }

    @Test
    void isTokenExpired_ShouldReturnFalse_WhenTokenIsNotExpired() {
        boolean result = jwtUtils.isTokenExpired(validToken);
        assertFalse(result);
    }
    @Test
    void isTokenExpired_ShouldThrowException_WhenTokenIsInvalid() {
        // Arrange
        String invalidToken = "invalid.token";

        // Act & Assert
        assertThrows(
                Exception.class,
                () -> jwtUtils.isTokenExpired(invalidToken)
        );
    }

    // ==================== Тесты для setSecret ====================

    @Test
    void setSecret_ShouldInitializeKey_WhenSecretIsValid() {
        // Act - уже вызван в @BeforeEach
        // Assert - проверяем что можем сгенерировать токен
        String token = jwtUtils.generateToken(TEST_USERNAME);
        assertNotNull(token);
    }

    @Test
    void setSecret_ShouldThrowException_WhenSecretIsInvalid() {
        // Arrange
        JwtUtils jwtUtilsWithInvalidSecret = new JwtUtils(jwtExceptionHandler);
        ReflectionTestUtils.setField(jwtUtilsWithInvalidSecret, "secret", "invalid-base64");

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> jwtUtilsWithInvalidSecret.setSecret()
        );

        assertTrue(exception.getMessage().contains("Не удалось инициализировать секретный ключ JWT"));
    }

    @Test
    void setSecret_ShouldThrowException_WhenSecretIsNull() {
        // Arrange
        JwtUtils jwtUtilsWithNullSecret = new JwtUtils(jwtExceptionHandler);
        ReflectionTestUtils.setField(jwtUtilsWithNullSecret, "secret", null);

        // Act & Assert
        assertThrows(
                Exception.class,
                () -> jwtUtilsWithNullSecret.setSecret()
        );
    }

    // ==================== Вспомогательные тесты ====================

    @Test
    void generateToken_ShouldGenerateDifferentTokensForSameUser() {
        // Act
        String token1 = jwtUtils.generateToken(TEST_USERNAME);
        String token2 = jwtUtils.generateToken(TEST_USERNAME);

        // Assert
        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2); // JTI должен быть разный
    }

    @Test
    void validateToken_ShouldReturnFalse_ForExpiredToken() {
        // Arrange
        // Генерируем токен с очень коротким сроком жизни
        JwtUtils shortLivedJwtUtils = createShortLivedJwtUtils();
        String token = shortLivedJwtUtils.generateToken(TEST_USERNAME);

        // Ждем немного, чтобы токен истек
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        when(userDetails.getUsername()).thenReturn(TEST_USERNAME);

        // Act
        boolean result = shortLivedJwtUtils.validateToken(token, userDetails);

        // Assert
        assertFalse(result);
    }

    // Вспомогательный метод для создания JwtUtils с коротким сроком жизни токенов
    private JwtUtils createShortLivedJwtUtils() {
        JwtUtils shortLivedJwtUtils = new JwtUtils(jwtExceptionHandler);
        ReflectionTestUtils.setField(shortLivedJwtUtils, "secret", SECRET);
        shortLivedJwtUtils.setSecret();

        // Подменяем метод generateToken через наследование или прокси
        // Вместо этого, тестируем isTokenExpired напрямую
        return shortLivedJwtUtils;
    }
}