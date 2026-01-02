package com.example.kwizi.controller;

import com.example.kwizi.DTO.request.AuthenticationRequest;
import com.example.kwizi.DTO.request.ChangePasswordRequest;
import com.example.kwizi.DTO.request.RegistrationRequest;
import com.example.kwizi.DTO.response.ApiResponse;
import com.example.kwizi.DTO.response.AuthenticationResponse;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.RevokedTokenRepository;
import com.example.kwizi.security.JwtUtils;
import com.example.kwizi.security.UserDetailsImpl;
import com.example.kwizi.service.AuthenticationService;
import com.example.kwizi.service.RegistrationService;
import com.example.kwizi.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("AuthenticationController тесты")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthenticationControllerTest {

    @Mock
    private Authentication authentication;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private RegistrationService registrationService;

    @Mock
    private UserService userService;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private RevokedTokenRepository revokedTokenRepo;

    @Mock
    private UserDetailsImpl userDetails;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private AuthenticationController authenticationController;

    private static final String TEST_USERNAME = "testUser";
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_JWT_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
    private static final String TEST_NEW_PASSWORD = "newPassword123";

    @BeforeEach
    void setUp() {
        when(userDetails.getUsername()).thenReturn(TEST_USERNAME);
    }

    @Nested
    @DisplayName("Смена пароля")
    class ChangePasswordTests {

        @Test
        @DisplayName("Успешная смена пароля")
        void changePassword_Success() {
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setOldPassword(TEST_PASSWORD);
            request.setNewPassword(TEST_NEW_PASSWORD);

            doNothing().when(authenticationService)
                    .changePassword(eq(TEST_USERNAME), eq(request));

            ResponseEntity<?> response = authenticationController.changePassword(request, userDetails);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            ApiResponse<?> apiResponse = extractApiResponse(response);
            assertThat(apiResponse.isSuccess()).isTrue();
            assertThat(apiResponse.getMessage()).isEqualTo("Пароль успешно изменен");

            verify(authenticationService, times(1))
                    .changePassword(TEST_USERNAME, request);
        }

        @Test
        @DisplayName("Смена пароля с неверным старым паролем")
        void changePassword_WrongOldPassword_ThrowsException() {
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setOldPassword("wrongPassword");
            request.setNewPassword(TEST_NEW_PASSWORD);

            doThrow(new IllegalArgumentException("Неверный старый пароль"))
                    .when(authenticationService).changePassword(eq(TEST_USERNAME), eq(request));

            assertThatThrownBy(() -> authenticationController.changePassword(request, userDetails))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Неверный старый пароль");
        }

        @Test
        @DisplayName("Смена пароля на тот же самый")
        void changePassword_SamePassword_ThrowsException() {
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setOldPassword(TEST_PASSWORD);
            request.setNewPassword(TEST_PASSWORD);

            doThrow(new IllegalArgumentException("Новый пароль не должен совпадать со старым"))
                    .when(authenticationService).changePassword(eq(TEST_USERNAME), eq(request));

            assertThatThrownBy(() -> authenticationController.changePassword(request, userDetails))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Новый пароль не должен совпадать со старым");
        }

        @Test
        @DisplayName("Смена пароля со слабым новым паролем")
        void changePassword_WeakNewPassword_ThrowsException() {
            ChangePasswordRequest request = new ChangePasswordRequest();
            request.setOldPassword(TEST_PASSWORD);
            request.setNewPassword("123");

            doThrow(new IllegalArgumentException("Пароль слишком слабый"))
                    .when(authenticationService).changePassword(eq(TEST_USERNAME), eq(request));

            assertThatThrownBy(() -> authenticationController.changePassword(request, userDetails))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Пароль слишком слабый");
        }
    }

    @Nested
    @DisplayName("Регистрация пользователя")
    class RegistrationTests {

        @Test
        @DisplayName("Успешная регистрация пользователя")
        void registerUser_Success() {
            RegistrationRequest request = new RegistrationRequest();
            request.setUsername(TEST_USERNAME);
            request.setPassword(TEST_PASSWORD);
            request.setEmail("test@example.com");
            request.setFirstName("John");
            request.setLastName("Doe");

            doNothing().when(registrationService)
                    .registerUser(eq(request));

            ResponseEntity<?> response = authenticationController.registerUser(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            ApiResponse<?> apiResponse = extractApiResponse(response);
            assertThat(apiResponse.isSuccess()).isTrue();
            assertThat(apiResponse.getMessage()).isEqualTo("Пользователь успешно зарегистрирован");

            verify(registrationService, times(1))
                    .registerUser(request);
        }

        @Test
        @DisplayName("Регистрация с уже существующим username")
        void registerUser_UsernameAlreadyExists_ThrowsException() {
            RegistrationRequest request = new RegistrationRequest();
            request.setUsername(TEST_USERNAME);
            request.setPassword(TEST_PASSWORD);
            request.setEmail("test@example.com");

            doThrow(new DuplicateKeyException("Пользователь с таким именем уже существует"))
                    .when(registrationService).registerUser(eq(request));

            assertThatThrownBy(() -> authenticationController.registerUser(request))
                    .isInstanceOf(DuplicateKeyException.class)
                    .hasMessageContaining("Пользователь с таким именем уже существует");
        }

        @Test
        @DisplayName("Регистрация с уже существующим email")
        void registerUser_EmailAlreadyExists_ThrowsException() {
            RegistrationRequest request = new RegistrationRequest();
            request.setUsername("newUser");
            request.setPassword(TEST_PASSWORD);
            request.setEmail("existing@example.com");

            doThrow(new DuplicateKeyException("Пользователь с таким email уже существует"))
                    .when(registrationService).registerUser(eq(request));

            assertThatThrownBy(() -> authenticationController.registerUser(request))
                    .isInstanceOf(DuplicateKeyException.class)
                    .hasMessageContaining("Пользователь с таким email уже существует");
        }

        @Test
        @DisplayName("Регистрация с невалидным email")
        void registerUser_InvalidEmail_ThrowsException() {
            RegistrationRequest request = new RegistrationRequest();
            request.setUsername("newUser");
            request.setPassword(TEST_PASSWORD);
            request.setEmail("invalid-email");

            doThrow(new IllegalArgumentException("Невалидный email"))
                    .when(registrationService).registerUser(eq(request));

            assertThatThrownBy(() -> authenticationController.registerUser(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Невалидный email");
        }
    }

    @Nested
    @DisplayName("Аутентификация (логин)")
    class LoginTests {

        @Test
        @DisplayName("Успешная аутентификация")
        void login_Success() {
            AuthenticationRequest request = new AuthenticationRequest();
            request.setUsername(TEST_USERNAME);
            request.setPassword(TEST_PASSWORD);

            UserDetails userDetails = mock(UserDetails.class);
            when(userDetails.getUsername()).thenReturn(TEST_USERNAME);

            User mockUser = new User();
            mockUser.setId(TEST_USER_ID);
            mockUser.setUsername(TEST_USERNAME);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);

            when(userDetailsService.loadUserByUsername(TEST_USERNAME))
                    .thenReturn(userDetails);

            when(userService.findByUsername(TEST_USERNAME))
                    .thenReturn(Optional.of(mockUser));

            when(jwtUtils.generateToken(TEST_USERNAME, TEST_USER_ID))
                    .thenReturn(TEST_JWT_TOKEN);

            ResponseEntity<?> response = authenticationController.login(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            AuthenticationResponse authResponse = (AuthenticationResponse) response.getBody();
            assertThat(authResponse).isNotNull();
            assertThat(authResponse.getJwt()).isEqualTo(TEST_JWT_TOKEN);

            verify(authenticationManager, times(1))
                    .authenticate(new UsernamePasswordAuthenticationToken(TEST_USERNAME, TEST_PASSWORD));
            verify(userDetailsService, times(1)).loadUserByUsername(TEST_USERNAME);
            verify(userService, times(1)).findByUsername(TEST_USERNAME);
            verify(jwtUtils, times(1)).generateToken(TEST_USERNAME, TEST_USER_ID);
        }

        @Test
        @DisplayName("Аутентификация с неверными учетными данными")
        void login_InvalidCredentials_ThrowsException() {
            AuthenticationRequest request = new AuthenticationRequest();
            request.setUsername(TEST_USERNAME);
            request.setPassword("wrongPassword");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Неверные учетные данные"));

            assertThatThrownBy(() -> authenticationController.login(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Неверные учетные данные");
        }

        @Test
        @DisplayName("Аутентификация несуществующего пользователя")
        void login_UserNotFound_ThrowsException() {
            AuthenticationRequest request = new AuthenticationRequest();
            request.setUsername("nonExistentUser");
            request.setPassword(TEST_PASSWORD);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);

            when(userDetailsService.loadUserByUsername("nonExistentUser"))
                    .thenThrow(new UsernameNotFoundException("Пользователь не найден"));

            assertThatThrownBy(() -> authenticationController.login(request))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("Пользователь не найден");
        }
    }

    @Nested
    @DisplayName("Выход из системы (логаут)")
    class LogoutTests {

        @Test
        @DisplayName("Успешный выход из системы")
        void logout_Success() {
            when(jwtUtils.extractToken(httpServletRequest))
                    .thenReturn(TEST_JWT_TOKEN);

            doNothing().when(authenticationService)
                    .logout(eq(TEST_JWT_TOKEN));

            ResponseEntity<ApiResponse<Void>> response =
                    authenticationController.logout(httpServletRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            ApiResponse<Void> apiResponse = response.getBody();
            assertThat(apiResponse.isSuccess()).isTrue();
            assertThat(apiResponse.getMessage()).isEqualTo("Выход выполнен успешно");

            verify(jwtUtils, times(1)).extractToken(httpServletRequest);
            verify(authenticationService, times(1)).logout(TEST_JWT_TOKEN);
        }

        @Test
        @DisplayName("Выход без токена")
        void logout_NoToken_ReturnsBadRequest() {
            when(jwtUtils.extractToken(httpServletRequest))
                    .thenReturn(null);

            ResponseEntity<ApiResponse<Void>> response =
                    authenticationController.logout(httpServletRequest);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

            ApiResponse<Void> apiResponse = response.getBody();
            assertThat(apiResponse.isSuccess()).isFalse();
            assertThat(apiResponse.getMessage()).isEqualTo("Токен отсутствует");

            verify(authenticationService, never()).logout(anyString());
        }

        @Test
        @DisplayName("Выход с истекшим токеном")
        void logout_ExpiredToken_Success() {
            when(jwtUtils.extractToken(httpServletRequest))
                    .thenReturn(TEST_JWT_TOKEN);

            doThrow(new IllegalArgumentException("Токен истек"))
                    .when(authenticationService).logout(eq(TEST_JWT_TOKEN));

            assertThatThrownBy(() -> authenticationController.logout(httpServletRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Токен истек");
        }

        @Test
        @DisplayName("Выход с уже отозванным токеном")
        void logout_AlreadyRevokedToken_Success() {
            when(jwtUtils.extractToken(httpServletRequest))
                    .thenReturn(TEST_JWT_TOKEN);

            doThrow(new IllegalStateException("Токен уже отозван"))
                    .when(authenticationService).logout(eq(TEST_JWT_TOKEN));

            assertThatThrownBy(() -> authenticationController.logout(httpServletRequest))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Токен уже отозван");
        }
    }

    @SuppressWarnings("unchecked")
    private <T> ApiResponse<T> extractApiResponse(ResponseEntity<?> responseEntity) {
        return (ApiResponse<T>) responseEntity.getBody();
    }
}