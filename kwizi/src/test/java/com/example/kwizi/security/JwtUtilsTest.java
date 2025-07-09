package com.example.kwizi.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.kwizi.exception.JwtAuthenticationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class JwtUtilsTest {
    private static final String INVALID_SECRET = "invalidSecret"; // This will cause an error

    @Mock
    private HttpServletRequest request;

    @Mock
    private UserDetails userDetails;

    private JwtUtils jwtUtils;
    private Key testKey;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtils = new JwtUtils();

        // Устанавливаем приватные поля через reflection
        setPrivateField(jwtUtils, "secret", "mySecretKeyWhichIsVeryLongSoItCanBeUsedForHS512Algorithm");
        jwtUtils.setSecret(); // Инициализируем ключ

        // Сохраняем ключ для использования в тестах
        this.testKey = (Key) getPrivateField(jwtUtils, "key");
    }

    // Вспомогательные методы для работы с приватными полями
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object getPrivateField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    @Test
    void setSecret_ShouldThrowException_WhenSecretIsInvalid() {
        // Arrange
        JwtUtils jwtUtilsInvalid = new JwtUtils();  // Create new instance for this test
        ReflectionTestUtils.setField(jwtUtilsInvalid, "secret", INVALID_SECRET);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> jwtUtilsInvalid.setSecret());
    }
    @Test
    void validateToken_ShouldReturnFalse_ForExpiredToken() {
        // Arrange
        String expiredToken = generateExpiredToken();
        when(userDetails.getUsername()).thenReturn("testuser");

        // Act & Assert
        assertThrows(JwtAuthenticationException.class,
                () -> jwtUtils.validateToken(expiredToken, userDetails));

        // Или проверить через try-catch
        try {
            jwtUtils.validateToken(expiredToken, userDetails);
            fail("Expected JwtAuthenticationException");
        } catch (JwtAuthenticationException e) {
            // Ожидаемое исключение
        }
    }

    @Test
    void setSecret_ShouldInitializeKey() throws Exception {
        Key key = (Key) getPrivateField(jwtUtils, "key");
        assertNotNull(key);
    }

    @Test
    void generateToken_ShouldReturnValidToken() {
        String token = jwtUtils.generateToken("testuser");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void extractToken_ShouldReturnToken_WhenValidHeader() {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.token");
        assertEquals("valid.token", jwtUtils.extractToken(request));
    }

    @Test
    void extractToken_ShouldReturnNull_WhenNoBearerToken() {
        when(request.getHeader("Authorization")).thenReturn("Invalid");
        assertNull(jwtUtils.extractToken(request));
    }

    @Test
    void extractToken_ShouldReturnNull_WhenNoHeader() {
        when(request.getHeader("Authorization")).thenReturn(null);
        assertNull(jwtUtils.extractToken(request));
    }

    @Test
    void getUsernameFromToken_ShouldReturnUsername() {
        String token = jwtUtils.generateToken("testuser");
        assertEquals("testuser", jwtUtils.getUsernameFromToken(token));
    }

    @Test
    void getUsernameFromToken_ShouldThrow_WhenTokenExpired() {
        String expiredToken = generateExpiredToken();
        assertThrows(JwtAuthenticationException.class,
                () -> jwtUtils.getUsernameFromToken(expiredToken));
    }

    @Test
    void getUsernameFromToken_ShouldThrow_WhenInvalidToken() {
        assertThrows(JwtAuthenticationException.class,
                () -> jwtUtils.getUsernameFromToken("invalid.token"));
    }

    @Test
    void extractJti_ShouldReturnJti() {
        String token = jwtUtils.generateToken("testuser");
        assertNotNull(jwtUtils.extractJti(token));
    }

    @Test
    void extractJti_ShouldThrow_WhenInvalidToken() {
        assertThrows(JwtAuthenticationException.class,
                () -> jwtUtils.extractJti("invalid.token"));
    }

    @Test
    void extractExpiration_ShouldReturnFutureDate() {
        String token = jwtUtils.generateToken("testuser");
        assertTrue(jwtUtils.extractExpiration(token).after(new Date()));
    }

    @Test
    void extractExpiration_ShouldThrow_WhenInvalidToken() {
        assertThrows(JwtAuthenticationException.class,
                () -> jwtUtils.extractExpiration("invalid.token"));
    }

    @Test
    void validateToken_ShouldReturnTrue_ForValidToken() {
        String token = jwtUtils.generateToken("testuser");
        when(userDetails.getUsername()).thenReturn("testuser");
        assertTrue(jwtUtils.validateToken(token, userDetails));
    }

    @Test
    void validateToken_ShouldReturnFalse_WhenUsernameMismatch() {
        String token = jwtUtils.generateToken("testuser");
        when(userDetails.getUsername()).thenReturn("otheruser");
        assertFalse(jwtUtils.validateToken(token, userDetails));
    }

    @Test
    void isTokenExpired_ShouldReturnFalse_ForValidToken() {
        String token = jwtUtils.generateToken("testuser");
        assertFalse(jwtUtils.isTokenExpired(token));
    }

    @Test
    void isTokenExpired_ShouldReturnTrue_ForExpiredToken() {
        String expiredToken = generateExpiredToken();
        assertTrue(jwtUtils.isTokenExpired(expiredToken));
    }

    private String generateExpiredToken() {
        return Jwts.builder()
                .setSubject("testuser")
                .setIssuedAt(new Date(System.currentTimeMillis() - 10000))
                .setExpiration(new Date(System.currentTimeMillis() - 5000))
                .signWith(testKey)
                .compact();
    }




}