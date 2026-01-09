package com.example.kwizi.controller;

import com.example.kwizi.DTO.request.*;
import com.example.kwizi.DTO.response.ApiResponseDto;
import com.example.kwizi.DTO.response.UserProfileResponse;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.security.UserDetailsImpl;
import com.example.kwizi.service.AuthenticationService;
import com.example.kwizi.service.UserService;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("UserController тесты")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserControllerTest {

    @Mock
    private AuthenticationService authService;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController controller;

    private UserDetailsImpl userDetails;
    private UserProfileResponse userProfileResponse;

    @BeforeEach
    void setUp() {
        userDetails = createUserDetails(1L, "testUser");
        userProfileResponse = createUserProfileResponse();
    }

    private UserDetailsImpl createUserDetails(Long id, String username) {
        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
        when(userDetails.getId()).thenReturn(id);
        when(userDetails.getUsername()).thenReturn(username);
        return userDetails;
    }

    private UserProfileResponse createUserProfileResponse() {
        UserProfileResponse response = new UserProfileResponse(2L,"otherUser",
                "John","Doe","Test bio","test@example.com");

        return response;
    }

    @Nested
    @DisplayName("Получение информации о пользователе")
    class GetUserInfoTests {

        @Test
        @DisplayName("Успешное получение пользователя по username")
        void getUserByUsername_Success() {
            
            String username = "otherUser";
            when(userService.findUsername(username, 1L)).thenReturn(userProfileResponse);

            
            var response = controller.getUserByUsername(username, userDetails);

            
            assertThat(response)
                    .extracting(
                            r -> r.getStatusCode().value(),
                            r -> r.getBody().isSuccess(),
                            r -> r.getBody().getMessage(),
                            r -> r.getBody().getData()
                    )
                    .containsExactly(200, true, "Пользователь найден", userProfileResponse);
        }

        @Test
        @DisplayName("Успешное получение профиля пользователя")
        void getUserProfile_Success() {
            
            when(userService.getUserProfile(1L)).thenReturn(userProfileResponse);

            
            var response = controller.getUserProfile(userDetails);

            
            assertThat(response)
                    .extracting(
                            r -> r.getStatusCode().value(),
                            r -> r.getBody().getMessage(),
                            r -> r.getBody().getData()
                    )
                    .containsExactly(200, "Профиль успешно загружен", userProfileResponse);
        }

        @Test
        @DisplayName("Получение статуса верификации email")
        void getEmailVerified_Success() {
            
            when(userService.getEmailVerified(1L)).thenReturn(true);

            
            var response = controller.getEmailVerified(userDetails);

            
            assertThat(response)
                    .extracting(
                            r -> r.getStatusCode().value(),
                            r -> r.getBody().getMessage(),
                            r -> r.getBody().getData()
                    )
                    .containsExactly(200, "Email подтвержден", true);
        }
    }

    @Nested
    @DisplayName("Верификация email")
    class EmailVerificationTests {

        @Test
        @DisplayName("Успешная верификация email пользователя")
        void verifyUserEmail_Success() {
            
            doNothing().when(userService).verifyUserEmail(1L);

            
            var response = controller.verifyUserEmail(userDetails);

            
            assertThat(response)
                    .extracting(
                            r -> r.getStatusCode().value(),
                            r -> r.getBody().getMessage()
                    )
                    .containsExactly(200, "Email успешно подтвержден");
        }

        @Test
        @DisplayName("Успешная отправка письма для верификации")
        void sendVerificationEmail_Success() {
            
            doNothing().when(authService).sendVerificationEmail(1L);

            
            var response = controller.sendVerificationEmail(userDetails);

            
            assertThat(response)
                    .extracting(
                            r -> r.getStatusCode().value(),
                            r -> r.getBody().getMessage()
                    )
                    .containsExactly(200, "Письмо для подтверждения email отправлено");
        }
    }

    @Nested
    @DisplayName("Обновление профиля пользователя")
    class UpdateProfileTests {

        @Test
        @DisplayName("Успешное обновление bio")
        void updateBio_Success() {
            
            UpdateBioRequest request = new UpdateBioRequest();
            request.setBio("New bio text");
            doNothing().when(userService).updateBio(1L, "New bio text");

            
            var response = controller.updateBio(request, userDetails);

            
            assertThat(response)
                    .extracting(
                            r -> r.getStatusCode().value(),
                            r -> r.getBody().getMessage()
                    )
                    .containsExactly(200, "Bio успешно обновлен");
        }

        @Test
        @DisplayName("Успешное обновление имени")
        void updateFirstName_Success() {
            
            UpdateFirstNameRequest request = new UpdateFirstNameRequest();
            request.setFirstName("NewFirstName");
            doNothing().when(userService).updateFirstName(1L, "NewFirstName");

            
            var response = controller.updateFirstName(request, userDetails);

            
            assertThat(response)
                    .extracting(
                            r -> r.getStatusCode().value(),
                            r -> r.getBody().getMessage()
                    )
                    .containsExactly(200, "Имя успешно обновлено");
        }

        @Test
        @DisplayName("Успешное обновление фамилии")
        void updateLastName_Success() {
            
            UpdateLastNameRequest request = new UpdateLastNameRequest();
            request.setLastName("NewLastName");
            doNothing().when(userService).updateLastName(1L, "NewLastName");

            
            var response = controller.updateLastName(request, userDetails);

            
            assertThat(response)
                    .extracting(
                            r -> r.getStatusCode().value(),
                            r -> r.getBody().getMessage()
                    )
                    .containsExactly(200, "Фамилия успешно обновлена");
        }

        @Test
        @DisplayName("Успешное обновление username")
        void updateUsername_Success() {
            
            UpdateUsernameRequest request = new UpdateUsernameRequest();
            request.setUsername("newUsername");
            when(userService.updateUsername(1L, "newUsername")).thenReturn("new-jwt-token");

            
            var response = controller.updateUsername(request, userDetails);

            
            assertThat(response)
                    .extracting(
                            r -> r.getStatusCode().value(),
                            r -> r.getBody().getMessage(),
                            r -> r.getBody().getData()
                    )
                    .containsExactly(
                            200,
                            "Никнейм успешно обновлен",
                            Map.of("token", "new-jwt-token")
                    );
        }

        @Test
        @DisplayName("Успешное обновление email")
        void updateEmail_Success() {
            
            UpdateEmailRequest request = new UpdateEmailRequest();
            request.setEmail("new@example.com");
            doNothing().when(userService).updateEmail(1L, "new@example.com");

            
            var response = controller.updateEmail(request, userDetails);

            
            assertThat(response)
                    .extracting(
                            r -> r.getStatusCode().value(),
                            r -> r.getBody().getMessage()
                    )
                    .containsExactly(200, "Email успешно обновлен");
        }

        @Test
        @DisplayName("Успешное обновление аватара")
        void updateAvatar_Success() throws IOException {
            
            MultipartFile file = mock(MultipartFile.class);
            doNothing().when(userService).updateUserAvatar(file, 1L);

            
            var response = controller.updateAvatar(file, userDetails);

            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Object body = response.getBody();
            assertThat(body).isInstanceOf(ApiResponseDto.class);
        }
    }

    @Nested
    @DisplayName("Обработка исключений")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("Пользователь не найден")
        void userNotFound() {
            
            String username = "nonExistentUser";
            when(userService.findUsername(username, 1L))
                    .thenThrow(new UserNotFoundException("Пользователь не найден"));

             
            assertThatThrownBy(() -> controller.getUserByUsername(username, userDetails))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("Пользователь не найден");
        }

        @Test
        @DisplayName("Невалидный email при обновлении")
        void invalidEmail() {
            
            UpdateEmailRequest request = new UpdateEmailRequest();
            request.setEmail("invalid-email");
            doThrow(new IllegalArgumentException("Invalid email format"))
                    .when(userService).updateEmail(1L, "invalid-email");

             
            assertThatThrownBy(() -> controller.updateEmail(request, userDetails))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid email format");
        }

        @Test
        @DisplayName("Username уже занят")
        void usernameAlreadyTaken() {
            
            UpdateUsernameRequest request = new UpdateUsernameRequest();
            request.setUsername("takenUsername");
            when(userService.updateUsername(1L, "takenUsername"))
                    .thenThrow(new DuplicateKeyException("Username уже занят"));

             
            assertThatThrownBy(() -> controller.updateUsername(request, userDetails))
                    .isInstanceOf(DuplicateKeyException.class)
                    .hasMessageContaining("Username уже занят");
        }
    }

}