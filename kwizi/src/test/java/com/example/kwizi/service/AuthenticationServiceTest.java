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
import com.example.kwizi.websocket.UniversalChatHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService тесты")
class AuthenticationServiceTest {

    @Mock private AuthenticationRepository authenticationRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtEmailVerify jwtEmailVerify;
    @Mock private EmailService emailService;
    @Mock private JwtUtils jwtUtils;
    @Mock private UserService userService;
    @Mock private RevokedTokenRepository revokedTokenRepo;
    @Mock private UniversalChatHandler universalChatHandler;

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
    
    @Nested
    @DisplayName("Выход из системы (logout)")
    class Logout {

        @Test
        @DisplayName("✅ Успешная деактивация токена")
        void success() {
            
            String token = "valid.jwt.token";
            String jti = "token-jti-123";
            Date expiresAt = new Date(System.currentTimeMillis() + 3600_000);

            when(jwtUtils.extractJti(token)).thenReturn(jti);
            when(jwtUtils.extractExpiration(token)).thenReturn(expiresAt);
            when(jwtUtils.getUsernameFromToken(token)).thenReturn("testuser");
            when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(revokedTokenRepo.save(any(RevokedToken.class))).thenReturn(new RevokedToken());

            
            authenticationService.logout(token);

            
            verify(jwtUtils).extractJti(token);
            verify(jwtUtils).extractExpiration(token);
            verify(jwtUtils).getUsernameFromToken(token);
            verify(userService).findByUsername("testuser");
            verify(revokedTokenRepo).save(any(RevokedToken.class));
        }

        @Test
        @DisplayName("Пользователь не найден")
        void userNotFound() {
            
            String token = "valid.jwt.token";
            when(jwtUtils.getUsernameFromToken(token)).thenReturn("nonexistent");
            when(userService.findByUsername("nonexistent")).thenReturn(Optional.empty());

             
            assertThatThrownBy(() -> authenticationService.logout(token))
                    .isInstanceOf(UserNotFoundException.class);

            verify(revokedTokenRepo, never()).save(any(RevokedToken.class));
        }
    }
    @Nested
    @DisplayName("Отправка email-подтверждения")
    class SendVerificationEmail {

        @Test
        @DisplayName("✅ Успешная отправка, если email не подтверждён")
        void success() {
            
            when(authenticationRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(jwtEmailVerify.generateVerificationToken(1L)).thenReturn("verification-token");
            doNothing().when(emailService).sendVerificationEmailAsync(anyString(), anyString());

            
            authenticationService.sendVerificationEmail(1L);

            
            verify(authenticationRepository).findById(1L);
            verify(jwtEmailVerify).generateVerificationToken(1L);
            verify(emailService).sendVerificationEmailAsync("test@example.com", "verification-token");
        }

        @Test
        @DisplayName("Пользователь не найден")
        void userNotFound() {
            
            when(authenticationRepository.findById(1L)).thenReturn(Optional.empty());

             
            assertThatThrownBy(() -> authenticationService.sendVerificationEmail(1L))
                    .isInstanceOf(UserNotFoundException.class);

            verify(jwtEmailVerify, never()).generateVerificationToken(anyLong());
            verify(emailService, never()).sendVerificationEmailAsync(anyString(), anyString());
        }

        @Test
        @DisplayName("Email уже подтверждён")
        void emailAlreadyVerified() {
            
            testUser.setEmail_verified(true);
            when(authenticationRepository.findById(1L)).thenReturn(Optional.of(testUser));

             
            assertThatThrownBy(() -> authenticationService.sendVerificationEmail(1L))
                    .isInstanceOf(EmailAlreadyVerifiedException.class);

            verify(jwtEmailVerify, never()).generateVerificationToken(anyLong());
            verify(emailService, never()).sendVerificationEmailAsync(anyString(), anyString());
        }
    }
    
    @Nested
    @DisplayName("Подтверждение email")
    class VerifyEmail {

        @Test
        @DisplayName("✅ Успешное подтверждение email")
        void success() {
            
            String token = "valid-verification-token";
            when(jwtEmailVerify.isTokenExpired(token)).thenReturn(false);
            when(jwtEmailVerify.getUserIdFromToken(token)).thenReturn("1");
            when(authenticationRepository.findById(1L)).thenReturn(Optional.of(testUser));

            
            authenticationService.verifyEmail(token);

            
            assertThat(testUser.isEmail_verified()).isTrue();
            verify(jwtEmailVerify).isTokenExpired(token);
            verify(jwtEmailVerify).getUserIdFromToken(token);
            verify(authenticationRepository).findById(1L);
        }

        @Test
        @DisplayName("Токен просрочен")
        void tokenExpired() {
            
            String token = "expired-token";
            when(jwtEmailVerify.isTokenExpired(token)).thenReturn(true);

             
            assertThatThrownBy(() -> authenticationService.verifyEmail(token))
                    .isInstanceOf(TokenExpiredException.class);

            verify(authenticationRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("Неверный формат ID в токене")
        void invalidUserIdFormat() {
            
            String token = "invalid-token";
            when(jwtEmailVerify.isTokenExpired(token)).thenReturn(false);
            when(jwtEmailVerify.getUserIdFromToken(token)).thenReturn("invalid-number");

             
            assertThatThrownBy(() -> authenticationService.verifyEmail(token))
                    .isInstanceOf(InvalidTokenException.class);

            verify(authenticationRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("Пользователь не найден по ID из токена")
        void userNotFound() {
            
            String token = "valid-token";
            when(jwtEmailVerify.isTokenExpired(token)).thenReturn(false);
            when(jwtEmailVerify.getUserIdFromToken(token)).thenReturn("999");
            when(authenticationRepository.findById(999L)).thenReturn(Optional.empty());

             
            assertThatThrownBy(() -> authenticationService.verifyEmail(token))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Смена пароля")
    class ChangePassword {

        @Test
        @DisplayName("✅ Успешная смена пароля")
        void success() {
            
            when(authenticationRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("oldPassword", "encodedOldPassword")).thenReturn(true);
            when(passwordEncoder.matches("newPassword123", "encodedOldPassword")).thenReturn(false);
            when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");

            
            authenticationService.changePassword("testuser", changePasswordRequest);

            
            assertThat(testUser.getPassword()).isEqualTo("encodedNewPassword");
            verify(passwordEncoder).encode("newPassword123");
        }

        @Test
        @DisplayName("Пользователь не найден")
        void userNotFound() {
            
            when(authenticationRepository.findByUsername("unknown")).thenReturn(Optional.empty());

             
            assertThatThrownBy(() -> authenticationService.changePassword("unknown", changePasswordRequest))
                    .isInstanceOf(UsernameNotFoundException.class);

            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("Неверный старый пароль")
        void invalidOldPassword() {
            
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setOldPassword("wrongOld");
            req.setNewPassword("newPassword123");

            when(authenticationRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongOld", "encodedOldPassword")).thenReturn(false);

             
            assertThatThrownBy(() -> authenticationService.changePassword("testuser", req))
                    .isInstanceOf(InvalidPasswordException.class);

            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("Новый пароль совпадает со старым")
        void newPasswordSameAsOld() {
            
            ChangePasswordRequest req = new ChangePasswordRequest();
            req.setOldPassword("oldPassword");
            req.setNewPassword("oldPassword");

            when(authenticationRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("oldPassword", "encodedOldPassword")).thenReturn(true);
            when(passwordEncoder.matches("oldPassword", "encodedOldPassword")).thenReturn(true);

             
            assertThatThrownBy(() -> authenticationService.changePassword("testuser", req))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("Ошибка кодирования нового пароля")
        void passwordEncodingFails() {
            
            when(authenticationRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("oldPassword", "encodedOldPassword")).thenReturn(true);
            when(passwordEncoder.matches("newPassword123", "encodedOldPassword")).thenReturn(false);
            when(passwordEncoder.encode("newPassword123"))
                    .thenThrow(new IllegalArgumentException("Password too weak"));

             
            assertThatThrownBy(() -> authenticationService.changePassword("testuser", changePasswordRequest))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Регистрация пользователя")
    class RegisterUser {

        @Test
        @DisplayName("✅ Успешная регистрация с кодированием пароля")
        void success() {
            
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

            
            User result = authenticationService.registerUser(newUser);

            
            assertThat(result).isNotNull();
            assertThat(newUser.getPassword()).isEqualTo("encodedPassword");
            verify(passwordEncoder).encode("plainPassword");
            verify(authenticationRepository).save(newUser);
        }
    }

    @Nested
    @DisplayName("Проверка существования пользователя")
    class ExistenceChecks {

        @Test
        @DisplayName("✅ Пользователь существует по имени")
        void existsByUsername_True() {
            
            when(authenticationRepository.existsByUsername("existinguser")).thenReturn(true);

            
            boolean result = authenticationService.existsByUsername("existinguser");

            
            assertThat(result).isTrue();
            verify(authenticationRepository).existsByUsername("existinguser");
        }

        @Test
        @DisplayName("Пользователь не существует по имени")
        void existsByUsername_False() {
            
            when(authenticationRepository.existsByUsername("nonexistent")).thenReturn(false);

            
            boolean result = authenticationService.existsByUsername("nonexistent");

            
            assertThat(result).isFalse();
            verify(authenticationRepository).existsByUsername("nonexistent");
        }

        @Test
        @DisplayName("✅ Пользователь найден по email")
        void findByEmail_Found() {
            
            when(authenticationRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            
            Optional<User> result = authenticationService.findByEmail("test@example.com");

            
            assertThat(result).isPresent().contains(testUser);
            verify(authenticationRepository).findByEmail("test@example.com");
        }

        @Test
        @DisplayName("Пользователь не найден по email")
        void findByEmail_NotFound() {
            
            when(authenticationRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            
            Optional<User> result = authenticationService.findByEmail("unknown@example.com");

            
            assertThat(result).isEmpty();
            verify(authenticationRepository).findByEmail("unknown@example.com");
        }
    }
}