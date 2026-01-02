package com.example.kwizi.service;

import com.example.kwizi.DTO.request.RegistrationRequest;
import com.example.kwizi.exception.EmailAlreadyExistsException;
import com.example.kwizi.exception.UsernameAlreadyExistsException;
import com.example.kwizi.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrationService тесты")
class RegistrationServiceTest {

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private RegistrationService registrationService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private final String USERNAME = "testuser";
    private final String EMAIL = "test@example.com";
    private final String PASSWORD = "password123";

    @Nested
    @DisplayName("Успешная регистрация")
    class SuccessfulRegistrationTests {

        @Test
        @DisplayName("Регистрация с полными данными")
        void registerUser_WithFullData_ShouldRegisterSuccessfully() {
            
            RegistrationRequest request = createFullRegistrationRequest();

            when(authenticationService.existsByUsername(USERNAME)).thenReturn(false);
            when(authenticationService.findByEmail(EMAIL)).thenReturn(Optional.empty());
            
            registrationService.registerUser(request);
            
            verify(authenticationService).registerUser(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            assertThat(savedUser)
                    .as("Должен сохранить пользователя со всеми данными")
                    .extracting(
                            User::getUsername,
                            User::getEmail,
                            User::getPassword,
                            User::getFirstName,
                            User::getLastName,
                            User::getBio
                    )
                    .containsExactly(
                            USERNAME,
                            EMAIL,
                            PASSWORD,
                            "John",
                            "Doe",
                            "Test bio"
                    );

            verify(authenticationService).existsByUsername(USERNAME);
            verify(authenticationService).findByEmail(EMAIL);
        }

        @Test
        @DisplayName("Регистрация с минимальными данными")
        void registerUser_WithMinimalData_ShouldRegisterSuccessfully() {
            
            RegistrationRequest request = createMinimalRegistrationRequest();

            when(authenticationService.existsByUsername(USERNAME)).thenReturn(false);
            when(authenticationService.findByEmail(EMAIL)).thenReturn(Optional.empty());
            
            registrationService.registerUser(request);
            
            verify(authenticationService).registerUser(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            assertThat(savedUser)
                    .as("Должен сохранить пользователя с только обязательными полями")
                    .extracting(
                            User::getUsername,
                            User::getEmail,
                            User::getPassword,
                            User::getFirstName,
                            User::getLastName,
                            User::getBio
                    )
                    .containsExactly(
                            USERNAME,
                            EMAIL,
                            PASSWORD,
                            null,
                            null,
                            null
                    );
        }
    }

    @Nested
    @DisplayName("Валидация уникальности")
    class ValidationTests {

        @Test
        @DisplayName("Существующий username выбрасывает исключение")
        void registerUser_WhenUsernameAlreadyExists_ShouldThrowUsernameAlreadyExistsException() {
            
            RegistrationRequest request = createFullRegistrationRequest();

            when(authenticationService.existsByUsername(USERNAME)).thenReturn(true);

            assertThatThrownBy(() -> registrationService.registerUser(request))
                    .as("Должно выбросить исключение для существующего username")
                    .isInstanceOf(UsernameAlreadyExistsException.class)
                    .hasMessage("Пользователь с таким username уже существует");

            verify(authenticationService, never()).registerUser(any());
            verify(authenticationService, never()).findByEmail(any());
        }

        @Test
        @DisplayName("Существующий email выбрасывает исключение")
        void registerUser_WhenEmailAlreadyExists_ShouldThrowEmailAlreadyExistsException() {
            
            RegistrationRequest request = createFullRegistrationRequest();
            User existingUser = createTestUser();

            when(authenticationService.existsByUsername(USERNAME)).thenReturn(false);
            when(authenticationService.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser));

            assertThatThrownBy(() -> registrationService.registerUser(request))
                    .as("Должно выбросить исключение для существующего email")
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessage("Пользователь с таким email уже существует");

            verify(authenticationService, never()).registerUser(any());
        }

        @Test
        @DisplayName("Проверка username выполняется перед проверкой email")
        void registerUser_WhenBothExist_ShouldCheckUsernameFirst() {
            
            RegistrationRequest request = createFullRegistrationRequest();

            when(authenticationService.existsByUsername(USERNAME)).thenReturn(true);
             
            assertThatThrownBy(() -> registrationService.registerUser(request))
                    .as("Должно проверить username первым")
                    .isInstanceOf(UsernameAlreadyExistsException.class);

            verify(authenticationService, never()).findByEmail(any());
            verify(authenticationService, never()).registerUser(any());
        }
    }

    @Nested
    @DisplayName("Маппинг данных из запроса")
    class DataMappingTests {

        @Test
        @DisplayName("Корректный маппинг всех полей из запроса")
        void createUserFromRequest_ShouldMapAllFieldsCorrectly() {
            
            RegistrationRequest request = createFullRegistrationRequest();

            when(authenticationService.existsByUsername(USERNAME)).thenReturn(false);
            when(authenticationService.findByEmail(EMAIL)).thenReturn(Optional.empty());

            registrationService.registerUser(request);

            verify(authenticationService).registerUser(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            assertThat(savedUser)
                    .as("Все поля должны корректно маппиться из запроса")
                    .extracting(
                            User::getUsername,
                            User::getEmail,
                            User::getPassword
                    )
                    .containsExactly(
                            USERNAME,
                            EMAIL,
                            PASSWORD
                    );
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Обработка null и пустых опциональных полей")
        void createUserFromRequest_WithNullOrEmptyOptionalFields_ShouldHandleCorrectly(String value) {
            
            RegistrationRequest request = createFullRegistrationRequest();
            request.setFirstName(value);
            request.setLastName(value);
            request.setBio(value);

            when(authenticationService.existsByUsername(USERNAME)).thenReturn(false);
            when(authenticationService.findByEmail(EMAIL)).thenReturn(Optional.empty());

            
            registrationService.registerUser(request);

            
            verify(authenticationService).registerUser(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            if (value == null) {
                assertThat(savedUser.getFirstName()).isNull();
                assertThat(savedUser.getLastName()).isNull();
                assertThat(savedUser.getBio()).isNull();
            } else {
                assertThat(savedUser.getFirstName()).isEmpty();
                assertThat(savedUser.getLastName()).isEmpty();
                assertThat(savedUser.getBio()).isEmpty();
            }
        }
    }

    private RegistrationRequest createFullRegistrationRequest() {
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername(USERNAME);
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setBio("Test bio");
        return request;
    }

    private RegistrationRequest createMinimalRegistrationRequest() {
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername(USERNAME);
        request.setEmail(EMAIL);
        request.setPassword(PASSWORD);
        return request;
    }

    private User createTestUser() {
        User user = new User();
        user.setUsername(USERNAME);
        user.setEmail(EMAIL);
        return user;
    }
}