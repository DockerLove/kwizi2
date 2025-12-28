package com.example.kwizi.controller;

import com.example.kwizi.DTO.request.*;
import com.example.kwizi.DTO.response.ApiResponse;
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
        @DisplayName("✅ Успешное получение пользователя по username")
        void getUserByUsername_Success() {
            // given
            String username = "otherUser";
            when(userService.findUsername(username, 1L)).thenReturn(userProfileResponse);

            // when
            var response = controller.getUserByUsername(username, userDetails);

            // then
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
        @DisplayName("✅ Успешное получение профиля пользователя")
        void getUserProfile_Success() {
            // given
            when(userService.getUserProfile(1L)).thenReturn(userProfileResponse);

            // when
            var response = controller.getUserProfile(userDetails);

            // then
            assertThat(response)
                    .extracting(
                            r -> r.getStatusCode().value(),
                            r -> r.getBody().getMessage(),
                            r -> r.getBody().getData()
                    )
                    .containsExactly(200, "Профиль успешно загружен", userProfileResponse);
        }

        @Test
        @DisplayName("✅ Получение статуса верификации email")
        void getEmailVerified_Success() {
            // given
            when(userService.getEmailVerified(1L)).thenReturn(true);

            // when
            var response = controller.getEmailVerified(userDetails);

            // then
            assertThat(response)
                    .extracting(
                            r -> r.getStatusCode().value(),
                            r -> r.getBody().getMessage(),
                            r -> r.getBody().getData()
                    )
                    .containsExactly(200, "Получен статус проверки", true);
        }
    }

    @Nested
    @DisplayName("Верификация email")
    class EmailVerificationTests {

        @Test
        @DisplayName("✅ Успешная верификация email пользователя")
        void verifyUserEmail_Success() {
            // given
            doNothing().when(userService).verifyUserEmail(1L);

            // when
            var response = controller.verifyUserEmail(userDetails);

            // then
            assertThat(response)
                    .extracting(
                            r -> r.getStatusCode().value(),
                            r -> r.getBody().getMessage()
                    )
                    .containsExactly(200, "Email успешно подтвержден");
        }

        @Test
        @DisplayName("✅ Успешная отправка письма для верификации")
        void sendVerificationEmail_Success() {
            // given
            doNothing().when(authService).sendVerificationEmail(1L);

            // when
            var response = controller.sendVerificationEmail(userDetails);

            // then
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
        @DisplayName("✅ Успешное обновление bio")
        void updateBio_Success() {
            // given
            UpdateBioRequest request = new UpdateBioRequest();
            request.setBio("New bio text");
            doNothing().when(userService).updateBio(1L, "New bio text");

            // when
            var response = controller.updateBio(request, userDetails);

            // then
            assertThat(response)
                    .extracting(
                            r -> r.getStatusCode().value(),
                            r -> r.getBody().getMessage()
                    )
                    .containsExactly(200, "Bio успешно обновлен");
        }

        @Test
        @DisplayName("✅ Успешное обновление имени")
        void updateFirstName_Success() {
            // given
            UpdateFirstNameRequest request = new UpdateFirstNameRequest();
            request.setFirstName("NewFirstName");
            doNothing().when(userService).updateFirstName(1L, "NewFirstName");

            // when
            var response = controller.updateFirstName(request, userDetails);

            // then
            assertThat(response)
                    .extracting(
                            r -> r.getStatusCode().value(),
                            r -> r.getBody().getMessage()
                    )
                    .containsExactly(200, "Имя успешно обновлено");
        }

        @Test
        @DisplayName("✅ Успешное обновление фамилии")
        void updateLastName_Success() {
            // given
            UpdateLastNameRequest request = new UpdateLastNameRequest();
            request.setLastName("NewLastName");
            doNothing().when(userService).updateLastName(1L, "NewLastName");

            // when
            var response = controller.updateLastName(request, userDetails);

            // then
            assertThat(response)
                    .extracting(
                            r -> r.getStatusCode().value(),
                            r -> r.getBody().getMessage()
                    )
                    .containsExactly(200, "Фамилия успешно обновлена");
        }

        @Test
        @DisplayName("✅ Успешное обновление username")
        void updateUsername_Success() {
            // given
            UpdateUsernameRequest request = new UpdateUsernameRequest();
            request.setUsername("newUsername");
            when(userService.updateUsername(1L, "newUsername")).thenReturn("new-jwt-token");

            // when
            var response = controller.updateUsername(request, userDetails);

            // then
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
        @DisplayName("✅ Успешное обновление email")
        void updateEmail_Success() {
            // given
            UpdateEmailRequest request = new UpdateEmailRequest();
            request.setEmail("new@example.com");
            doNothing().when(userService).updateEmail(1L, "new@example.com");

            // when
            var response = controller.updateEmail(request, userDetails);

            // then
            assertThat(response)
                    .extracting(
                            r -> r.getStatusCode().value(),
                            r -> r.getBody().getMessage()
                    )
                    .containsExactly(200, "Email успешно обновлен");
        }

        @Test
        @DisplayName("✅ Успешное обновление аватара")
        void updateAvatar_Success() throws IOException {
            // given
            MultipartFile file = mock(MultipartFile.class);
            doNothing().when(userService).updateUserAvatar(file, 1L);

            // when
            var response = controller.updateAvatar(file, userDetails);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Object body = response.getBody();
            assertThat(body).isInstanceOf(ApiResponse.class);
        }
    }

    @Nested
    @DisplayName("Обработка исключений")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("❌ Пользователь не найден")
        void userNotFound() {
            // given
            String username = "nonExistentUser";
            when(userService.findUsername(username, 1L))
                    .thenThrow(new UserNotFoundException("Пользователь не найден"));

            // when & then
            assertThatThrownBy(() -> controller.getUserByUsername(username, userDetails))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("Пользователь не найден");
        }

        @Test
        @DisplayName("❌ Невалидный email при обновлении")
        void invalidEmail() {
            // given
            UpdateEmailRequest request = new UpdateEmailRequest();
            request.setEmail("invalid-email");
            doThrow(new IllegalArgumentException("Invalid email format"))
                    .when(userService).updateEmail(1L, "invalid-email");

            // when & then
            assertThatThrownBy(() -> controller.updateEmail(request, userDetails))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid email format");
        }

        @Test
        @DisplayName("❌ Username уже занят")
        void usernameAlreadyTaken() {
            // given
            UpdateUsernameRequest request = new UpdateUsernameRequest();
            request.setUsername("takenUsername");
            when(userService.updateUsername(1L, "takenUsername"))
                    .thenThrow(new DuplicateKeyException("Username уже занят"));

            // when & then
            assertThatThrownBy(() -> controller.updateUsername(request, userDetails))
                    .isInstanceOf(DuplicateKeyException.class)
                    .hasMessageContaining("Username уже занят");
        }
    }

}