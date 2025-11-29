package com.example.kwizi.service;

import com.example.kwizi.DTO.request.ChangePasswordRequest;
import com.example.kwizi.exception.AuthenticationService.EmailAlreadyVerifiedException;
import com.example.kwizi.exception.AuthenticationService.InvalidPasswordException;
import com.example.kwizi.exception.AuthenticationService.InvalidTokenException;
import com.example.kwizi.exception.AuthenticationService.TokenExpiredException;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.model.RevokedToken;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.AuthenticationRepository;
import com.example.kwizi.repository.RevokedTokenRepository;
import com.example.kwizi.security.JwtEmailVerify;
import com.example.kwizi.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthenticationRepository authenticationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtEmailVerify jwtEmailVerify;

    @Mock
    private EmailService emailService;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserService userService;

    @Mock
    private RevokedTokenRepository revokedTokenRepo;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User testUser;
    private ChangePasswordRequest changePasswordRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedOldPassword");
        testUser.setEmail_verified(false);

        changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setOldPassword("oldPassword");
        changePasswordRequest.setNewPassword("newPassword123");
    }

    // ✅ Тесты для logout
    @Test
    void logout_ShouldRevokeToken_WhenValidToken() {
        // Arrange
        String token = "valid.jwt.token";
        String jti = "token-jti-123";
        Date expiresAt = new Date(System.currentTimeMillis() + 3600000);

        when(jwtUtils.extractJti(token)).thenReturn(jti);
        when(jwtUtils.extractExpiration(token)).thenReturn(expiresAt);
        when(jwtUtils.getUsernameFromToken(token)).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(revokedTokenRepo.save(any(RevokedToken.class))).thenReturn(new RevokedToken());

        // Act
        authenticationService.logout(token);

        // Assert
        verify(jwtUtils).extractJti(token);
        verify(jwtUtils).extractExpiration(token);
        verify(jwtUtils).getUsernameFromToken(token);
        verify(userService).findByUsername("testuser");
        verify(revokedTokenRepo).save(any(RevokedToken.class));
    }

    @Test
    void logout_ShouldThrowUserNotFoundException_WhenUserNotFound() {
        // Arrange
        String token = "valid.jwt.token";

        when(jwtUtils.extractJti(token)).thenReturn("jti");
        when(jwtUtils.extractExpiration(token)).thenReturn(new Date());
        when(jwtUtils.getUsernameFromToken(token)).thenReturn("nonexistent");
        when(userService.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () ->
                authenticationService.logout(token));

        verify(revokedTokenRepo, never()).save(any(RevokedToken.class));
    }

    // ✅ Тесты для sendVerificationEmail
    @Test
    void sendVerificationEmail_ShouldSendEmail_WhenUserExistsAndEmailNotVerified() {
        // Arrange
        when(authenticationRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(jwtEmailVerify.generateVerificationToken(1L)).thenReturn("verification-token");
        doNothing().when(emailService).sendVerificationEmailAsync(anyString(), anyString());

        // Act
        authenticationService.sendVerificationEmail(1L);

        // Assert
        verify(authenticationRepository).findById(1L);
        verify(jwtEmailVerify).generateVerificationToken(1L);
        verify(emailService).sendVerificationEmailAsync("test@example.com", "verification-token");
    }

    @Test
    void sendVerificationEmail_ShouldThrowUserNotFoundException_WhenUserNotFound() {
        // Arrange
        when(authenticationRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () ->
                authenticationService.sendVerificationEmail(1L));

        verify(jwtEmailVerify, never()).generateVerificationToken(anyLong());
        verify(emailService, never()).sendVerificationEmailAsync(anyString(), anyString());
    }

    @Test
    void sendVerificationEmail_ShouldThrowEmailAlreadyVerifiedException_WhenEmailAlreadyVerified() {
        // Arrange
        testUser.setEmail_verified(true);
        when(authenticationRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(EmailAlreadyVerifiedException.class, () ->
                authenticationService.sendVerificationEmail(1L));

        verify(jwtEmailVerify, never()).generateVerificationToken(anyLong());
        verify(emailService, never()).sendVerificationEmailAsync(anyString(), anyString());
    }

    // ✅ Тесты для verifyEmail
    @Test
    void verifyEmail_ShouldVerifyEmail_WhenValidToken() {
        // Arrange
        String token = "valid-verification-token";

        when(jwtEmailVerify.isTokenExpired(token)).thenReturn(false);
        when(jwtEmailVerify.getUserIdFromToken(token)).thenReturn("1");
        when(authenticationRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        authenticationService.verifyEmail(token);

        // Assert
        assertTrue(testUser.isEmail_verified());
        verify(jwtEmailVerify).isTokenExpired(token);
        verify(jwtEmailVerify).getUserIdFromToken(token);
        verify(authenticationRepository).findById(1L);
    }

    @Test
    void verifyEmail_ShouldThrowTokenExpiredException_WhenTokenExpired() {
        // Arrange
        String token = "expired-token";
        when(jwtEmailVerify.isTokenExpired(token)).thenReturn(true);

        // Act & Assert
        assertThrows(TokenExpiredException.class, () ->
                authenticationService.verifyEmail(token));

        verify(authenticationRepository, never()).findById(anyLong());
    }

    @Test
    void verifyEmail_ShouldThrowInvalidTokenException_WhenInvalidUserIdFormat() {
        // Arrange
        String token = "invalid-token";
        when(jwtEmailVerify.isTokenExpired(token)).thenReturn(false);
        when(jwtEmailVerify.getUserIdFromToken(token)).thenReturn("invalid-number");

        // Act & Assert
        assertThrows(InvalidTokenException.class, () ->
                authenticationService.verifyEmail(token));

        verify(authenticationRepository, never()).findById(anyLong());
    }

    @Test
    void verifyEmail_ShouldThrowUserNotFoundException_WhenUserNotFound() {
        // Arrange
        String token = "valid-token";
        when(jwtEmailVerify.isTokenExpired(token)).thenReturn(false);
        when(jwtEmailVerify.getUserIdFromToken(token)).thenReturn("999");
        when(authenticationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () ->
                authenticationService.verifyEmail(token));
    }

    // ✅ Тесты для changePassword
    @Test
    void changePassword_ShouldChangePassword_WhenValidRequest() {
        // Arrange
        when(authenticationRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.matches("newPassword123", "encodedOldPassword")).thenReturn(false);
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");

        // Act
        authenticationService.changePassword("testuser", changePasswordRequest);

        // Assert
        verify(authenticationRepository).findByUsername("testuser");
        verify(passwordEncoder).matches("oldPassword", "encodedOldPassword");
        verify(passwordEncoder).matches("newPassword123", "encodedOldPassword");
        verify(passwordEncoder).encode("newPassword123");
        assertEquals("encodedNewPassword", testUser.getPassword());
    }

    @Test
    void changePassword_ShouldThrowUsernameNotFoundException_WhenUserNotFound() {
        // Arrange
        when(authenticationRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () ->
                authenticationService.changePassword("unknown", changePasswordRequest));

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void changePassword_ShouldThrowInvalidPasswordException_WhenOldPasswordIncorrect() {
        // Arrange
        when(authenticationRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongOldPassword", "encodedOldPassword")).thenReturn(false);

        ChangePasswordRequest wrongRequest = new ChangePasswordRequest();
        wrongRequest.setOldPassword("wrongOldPassword");
        wrongRequest.setNewPassword("newPassword123");

        // Act & Assert
        assertThrows(InvalidPasswordException.class, () ->
                authenticationService.changePassword("testuser", wrongRequest));

        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void changePassword_ShouldThrowIllegalArgumentException_WhenNewPasswordSameAsOld() {
        // Arrange
        when(authenticationRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.matches("oldPassword", "encodedOldPassword")).thenReturn(true);

        ChangePasswordRequest samePasswordRequest = new ChangePasswordRequest();
        samePasswordRequest.setOldPassword("oldPassword");
        samePasswordRequest.setNewPassword("oldPassword");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                authenticationService.changePassword("testuser", samePasswordRequest));

        verify(passwordEncoder, never()).encode(anyString());
    }

    // ✅ Тесты для registerUser
    @Test
    void registerUser_ShouldRegisterUserWithEncodedPassword() {
        // Arrange
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setPassword("plainPassword");
        newUser.setEmail("new@example.com");

        User savedUser = new User();
        savedUser.setId(2L);
        savedUser.setUsername("newuser");
        savedUser.setPassword("encodedPassword");

        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
        when(authenticationRepository.save(newUser)).thenReturn(savedUser);

        // Act
        User result = authenticationService.registerUser(newUser);

        // Assert
        assertNotNull(result);
        assertEquals("encodedPassword", newUser.getPassword()); // Password should be encoded
        verify(passwordEncoder).encode("plainPassword");
        verify(authenticationRepository).save(newUser);
    }

    // ✅ Тесты для existsByUsername
    @Test
    void existsByUsername_ShouldReturnTrue_WhenUserExists() {
        // Arrange
        when(authenticationRepository.existsByUsername("existinguser")).thenReturn(true);

        // Act
        boolean result = authenticationService.existsByUsername("existinguser");

        // Assert
        assertTrue(result);
        verify(authenticationRepository).existsByUsername("existinguser");
    }

    @Test
    void existsByUsername_ShouldReturnFalse_WhenUserNotExists() {
        // Arrange
        when(authenticationRepository.existsByUsername("nonexistent")).thenReturn(false);

        // Act
        boolean result = authenticationService.existsByUsername("nonexistent");

        // Assert
        assertFalse(result);
        verify(authenticationRepository).existsByUsername("nonexistent");
    }

    // ✅ Тесты для findByEmail
    @Test
    void findByEmail_ShouldReturnUser_WhenUserExists() {
        // Arrange
        when(authenticationRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = authenticationService.findByEmail("test@example.com");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(authenticationRepository).findByEmail("test@example.com");
    }

    @Test
    void findByEmail_ShouldReturnEmpty_WhenUserNotExists() {
        // Arrange
        when(authenticationRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        // Act
        Optional<User> result = authenticationService.findByEmail("unknown@example.com");

        // Assert
        assertTrue(result.isEmpty());
        verify(authenticationRepository).findByEmail("unknown@example.com");
    }

    @Test
    void changePassword_ShouldHandlePasswordEncodingException() {
        // Arrange
        when(authenticationRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.matches("newPassword123", "encodedOldPassword")).thenReturn(false);
        when(passwordEncoder.encode("newPassword123")).thenThrow(new IllegalArgumentException("Password too weak"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                authenticationService.changePassword("testuser", changePasswordRequest));
    }
}