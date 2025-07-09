package com.example.kwizi.security;
import com.example.kwizi.security.JwtEmailVerify;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class JwtEmailVerifyTest {

    private JwtEmailVerify jwtEmailVerify;

    private final String validSecret = "testsecrettestsecrettestsecrettestsecret"; // must be at least 32 bytes for HS256
    private final long validExpiration = 3600000; // 1 hour in milliseconds
    private Key testKey;

    @BeforeEach
    void setUp() {
        jwtEmailVerify = new JwtEmailVerify();
        // Set up a valid Key for tests
        testKey = Keys.hmacShaKeyFor(validSecret.getBytes());

        // Manually set fields using ReflectionTestUtils (since no Spring context)
        ReflectionTestUtils.setField(jwtEmailVerify, "secret", validSecret);
        ReflectionTestUtils.setField(jwtEmailVerify, "verificationTokenExpiration", validExpiration);

        // Call the setSecret method explicitly (since no Spring context to run @PostConstruct)
        jwtEmailVerify.setSecret();
        //Set the testKey to the created key
        ReflectionTestUtils.setField(jwtEmailVerify, "key", testKey);

    }

    @Test
    void generateVerificationToken_ShouldGenerateValidToken() {
        // Arrange
        Long userId = 123L;

        // Act
        String token = jwtEmailVerify.generateVerificationToken(userId);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void getUserIdFromToken_ShouldReturnUserId_WhenTokenIsValid() {
        // Arrange
        Long userId = 123L;
        String token = jwtEmailVerify.generateVerificationToken(userId);

        // Act
        String extractedUserId = jwtEmailVerify.getUserIdFromToken(token);

        // Assert
        assertEquals(String.valueOf(userId), extractedUserId);
    }

    @Test
    void getUserIdFromToken_ShouldReturnNull_WhenTokenIsInvalid() {
        // Arrange
        String invalidToken = "invalidToken";

        // Act
        String extractedUserId = jwtEmailVerify.getUserIdFromToken(invalidToken);

        // Assert
        assertNull(extractedUserId);
    }

    @Test
    void isTokenExpired_ShouldReturnFalse_WhenTokenIsNotExpired() {
        // Arrange
        Long userId = 123L;
        String token = jwtEmailVerify.generateVerificationToken(userId);

        // Act
        boolean isExpired = jwtEmailVerify.isTokenExpired(token);

        // Assert
        assertFalse(isExpired);
    }

    @Test
    void isTokenExpired_ShouldReturnTrue_WhenTokenIsExpired() throws InterruptedException {
        // Arrange
        JwtEmailVerify jwtEmailVerifyExpired = new JwtEmailVerify();

        //Manually set fields with expired token
        ReflectionTestUtils.setField(jwtEmailVerifyExpired, "secret", validSecret);
        ReflectionTestUtils.setField(jwtEmailVerifyExpired, "verificationTokenExpiration", 1);
        jwtEmailVerifyExpired.setSecret();

        Long userId = 123L;
        String token = jwtEmailVerifyExpired.generateVerificationToken(userId);

        // Act
        Thread.sleep(2); // Wait for token to expire

        boolean isExpired = jwtEmailVerifyExpired.isTokenExpired(token);

        // Assert
        assertTrue(isExpired);
    }

    @Test
    void isTokenExpired_ShouldReturnTrue_WhenTokenIsInvalid() {
        // Arrange
        String invalidToken = "invalidToken";

        // Act
        boolean isExpired = jwtEmailVerify.isTokenExpired(invalidToken);

        // Assert
        assertTrue(isExpired);
    }

    @Test
    void setSecret_ShouldNotThrowException_WhenSecretIsValid() {
        // Arrange and Act: Already done in the setup
        // Assert: No exception is thrown. The tests passes if no exception
    }

}