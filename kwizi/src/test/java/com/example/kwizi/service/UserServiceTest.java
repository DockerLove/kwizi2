package com.example.kwizi.service;

import com.example.kwizi.DTO.response.UserProfileResponse;
import com.example.kwizi.exception.BusinessLogicException;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.UserRepository;
import com.example.kwizi.security.JwtUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService тесты")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private UserService userService;

    private final Long EXISTING_USER_ID = 1L;
    private final Long ANOTHER_USER_ID = 2L;
    private final Long NON_EXISTENT_USER_ID = 999L;
    private final String OLD_USERNAME = "oldUser";
    private final String NEW_USERNAME = "newUser";
    private final String NEW_TOKEN = "new.jwt.token";

    private User createTestUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail("test@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPassword("password");
        return user;
    }

    @Nested
    @DisplayName("Основные сценарии обновления данных")
    class MainUpdateScenarios {

        @Test
        @DisplayName("✅ Успешное обновление username с возвратом нового токена")
        void updateUsername_WithValidNewUsername_ShouldUpdateAndReturnToken() {
            // given
            User existingUser = createTestUser(EXISTING_USER_ID, OLD_USERNAME);

            when(userRepository.existsByUsername(NEW_USERNAME)).thenReturn(false);
            when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(existingUser));
            when(jwtUtils.generateToken(NEW_USERNAME)).thenReturn(NEW_TOKEN);

            // when
            String resultToken = userService.updateUsername(EXISTING_USER_ID, NEW_USERNAME);

            // then
            assertThat(resultToken)
                    .as("Должен вернуться новый JWT токен")
                    .isEqualTo(NEW_TOKEN);

            assertThat(existingUser.getUsername())
                    .as("Username должен обновиться")
                    .isEqualTo(NEW_USERNAME);

            verify(userRepository).existsByUsername(NEW_USERNAME);
            verify(userRepository).findById(EXISTING_USER_ID);
            verify(jwtUtils).generateToken(NEW_USERNAME);
        }

        @Test
        @DisplayName("✅ Обновление email с сбросом верификации")
        void updateEmail_WithValidEmail_ShouldUpdateEmailAndResetVerification() {
            // given
            String newEmail = "newemail@example.com";
            User user = createTestUser(EXISTING_USER_ID, "testUser");
            user.setEmail("old@example.com");
            user.setEmail_verified(true);

            when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(user));

            // when
            userService.updateEmail(EXISTING_USER_ID, newEmail);

            // then
            assertThat(user.getEmail())
                    .as("Email должен обновиться")
                    .isEqualTo(newEmail);

            assertThat(user.isEmail_verified())
                    .as("Верификация должна сброситься")
                    .isFalse();

            verify(userRepository).findById(EXISTING_USER_ID);
        }

        @Test
        @DisplayName("✅ Обновление аватара пользователя")
        void updateUserAvatar_WithValidFile_ShouldUpdateAvatarUrl() {
            // given
            User user = createTestUser(EXISTING_USER_ID, "testUser");
            MultipartFile file = mock(MultipartFile.class);
            String avatarUrl = "http://example.com/avatars/user1.jpg";

            when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(user));
            when(file.isEmpty()).thenReturn(false);
            when(fileStorageService.saveUserAvatar(file, EXISTING_USER_ID)).thenReturn(avatarUrl);

            // when
            userService.updateUserAvatar(file, EXISTING_USER_ID);

            // then
            assertThat(user.getAvatarUrl())
                    .as("URL аватара должен обновиться")
                    .isEqualTo(avatarUrl);

            verify(userRepository).findById(EXISTING_USER_ID);
            verify(fileStorageService).saveUserAvatar(file, EXISTING_USER_ID);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("✅ Получение профиля пользователя")
        void getUserProfile_WithValidUser_ShouldReturnUserProfileResponse() {
            // given
            User user = createTestUser(EXISTING_USER_ID, "testuser");
            user.setFirstName("John");
            user.setLastName("Doe");
            user.setBio("Software Developer");
            user.setEmail("john@example.com");

            when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(user));

            // when
            UserProfileResponse result = userService.getUserProfile(EXISTING_USER_ID);

            // then
            assertThat(result)
                    .as("Ответ должен содержать все поля пользователя")
                    .extracting(
                            UserProfileResponse::getId,
                            UserProfileResponse::getFirstName,
                            UserProfileResponse::getLastName,
                            UserProfileResponse::getUsername,
                            UserProfileResponse::getBio,
                            UserProfileResponse::getEmail
                    )
                    .containsExactly(
                            EXISTING_USER_ID,
                            "John",
                            "Doe",
                            "testuser",
                            "Software Developer",
                            "john@example.com"
                    );

            verify(userRepository).findById(EXISTING_USER_ID);
        }

        @Test
        @DisplayName("✅ Поиск другого пользователя по username")
        void findUsername_WithDifferentUser_ShouldReturnUserProfile() {
            // given
            String username = "targetUser";
            User targetUser = createTestUser(ANOTHER_USER_ID, username);
            targetUser.setFirstName("John");
            targetUser.setLastName("Doe");
            targetUser.setBio("Test bio");
            targetUser.setEmail("target@example.com");

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(targetUser));

            // when
            UserProfileResponse result = userService.findUsername(username, EXISTING_USER_ID);

            // then
            assertThat(result)
                    .as("Должен вернуться профиль найденного пользователя")
                    .extracting(
                            UserProfileResponse::getId,
                            UserProfileResponse::getUsername
                    )
                    .containsExactly(ANOTHER_USER_ID, username);

            verify(userRepository).findByUsername(username);
        }
    }

    @Nested
    @DisplayName("Сценарии с ошибками")
    class ErrorScenarios {

        @Test
        @DisplayName("❌ Обновление username когда имя уже занято")
        void updateUsername_WhenUsernameAlreadyTaken_ShouldThrowException() {
            // given
            when(userRepository.existsByUsername(NEW_USERNAME)).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.updateUsername(EXISTING_USER_ID, NEW_USERNAME))
                    .as("Должно выбросить исключение при занятом username")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Имя пользователя занято");

            verify(userRepository).existsByUsername(NEW_USERNAME);
            verify(userRepository, never()).findById(anyLong());
            verify(jwtUtils, never()).generateToken(anyString());
        }

        @Test
        @DisplayName("❌ Обновление email когда пользователь не найден")
        void updateEmail_WhenUserNotFound_ShouldThrowUserNotFoundException() {
            // given
            String newEmail = "new@example.com";
            when(userRepository.findById(NON_EXISTENT_USER_ID)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.updateEmail(NON_EXISTENT_USER_ID, newEmail))
                    .as("Должно выбросить исключение при отсутствии пользователя")
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("Пользователь не найден");

            verify(userRepository).findById(NON_EXISTENT_USER_ID);
        }

        @Test
        @DisplayName("❌ Обновление аватара с пустым файлом")
        void updateUserAvatar_WithEmptyFile_ShouldThrowBusinessLogicException() {
            // given
            User user = createTestUser(EXISTING_USER_ID, "testUser");
            MultipartFile file = mock(MultipartFile.class);

            when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(user));
            when(file.isEmpty()).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.updateUserAvatar(file, EXISTING_USER_ID))
                    .as("Должно выбросить исключение при пустом файле")
                    .isInstanceOf(BusinessLogicException.class)
                    .hasMessage("Файл не может быть пустым");

            verify(userRepository).findById(EXISTING_USER_ID);
            verify(fileStorageService, never()).saveUserAvatar(any(), anyLong());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("❌ Поиск самого себя по username")
        void findUsername_WhenSearchingSelf_ShouldThrowException() {
            // given
            String username = "myUsername";
            User selfUser = createTestUser(EXISTING_USER_ID, username);
            when(userRepository.findByUsername(username)).thenReturn(Optional.of(selfUser));

            // when & then
            assertThatThrownBy(() -> userService.findUsername(username, EXISTING_USER_ID))
                    .as("Нельзя искать самого себя")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Нельзя искать самого себя");

            verify(userRepository).findByUsername(username);
        }

        @Test
        @DisplayName("❌ Верификация email когда email уже подтвержден")
        void verifyUserEmail_WhenEmailAlreadyVerified_ShouldThrowException() {
            // given
            User user = createTestUser(EXISTING_USER_ID, "testUser");
            user.setEmail_verified(true);
            when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> userService.verifyUserEmail(EXISTING_USER_ID))
                    .as("Должно выбросить исключение при уже верифицированном email")
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Email уже подтвержден");

            verify(userRepository).findById(EXISTING_USER_ID);
        }
    }

    @Nested
    @DisplayName("Параметризованные тесты для граничных значений")
    class ParameterizedTests {

        @ParameterizedTest
        @ValueSource(strings = {"Ан", "Иван", "Анна-Мария", "Jean Paul", "О'Коннор"})
        @DisplayName("Обновление firstName с разными валидными значениями")
        void updateFirstName_WithDifferentValidNames_ShouldUpdateSuccessfully(String firstName) {
            // given
            User user = createTestUser(EXISTING_USER_ID, "testUser");
            when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(user));

            // when
            userService.updateFirstName(EXISTING_USER_ID, firstName);

            // then
            assertThat(user.getFirstName())
                    .as("FirstName должен обновиться на: " + firstName)
                    .isEqualTo(firstName);

            verify(userRepository).findById(EXISTING_USER_ID);
        }

        @ParameterizedTest
        @ValueSource(strings = {"Smith", "Johnson-Williams", "Петров-Водкин", "O'Brien", "ван Дер Варт"})
        @DisplayName("Обновление lastName с разными валидными значениями")
        void updateLastName_WithDifferentValidLastNames_ShouldUpdateSuccessfully(String lastName) {
            // given
            User user = createTestUser(EXISTING_USER_ID, "testUser");
            when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(user));

            // when
            userService.updateLastName(EXISTING_USER_ID, lastName);

            // then
            assertThat(user.getLastName())
                    .as("LastName должен обновиться на: " + lastName)
                    .isEqualTo(lastName);

            verify(userRepository).findById(EXISTING_USER_ID);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "simple@example.com",
                "user.name@example.com",
                "user_name@example.com",
                "user+tag@example.com",
                "user@sub.domain.com",
                "user@example.co.uk"
        })
        @DisplayName("Обновление email с разными валидными форматами")
        void updateEmail_WithDifferentValidFormats_ShouldUpdateSuccessfully(String email) {
            // given
            User user = createTestUser(EXISTING_USER_ID, "testUser");
            user.setEmail_verified(true);
            when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(user));

            // when
            userService.updateEmail(EXISTING_USER_ID, email);

            // then
            assertThat(user.getEmail())
                    .as("Email должен обновиться на: " + email)
                    .isEqualTo(email);

            assertThat(user.isEmail_verified())
                    .as("Верификация должна сброситься")
                    .isFalse();

            verify(userRepository).findById(EXISTING_USER_ID);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Обновление bio с пустыми значениями")
        void updateBio_WithEmptyValues_ShouldUpdateSuccessfully(String bio) {
            // given
            User user = createTestUser(EXISTING_USER_ID, "testUser");
            user.setBio("Старое био");
            when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(user));

            // when
            userService.updateBio(EXISTING_USER_ID, bio);

            // then
            if (bio == null) {
                assertThat(user.getBio()).isNull();
            } else {
                assertThat(user.getBio()).isEmpty();
            }
            verify(userRepository).findById(EXISTING_USER_ID);
        }

        static Stream<Arguments> provideLongTexts() {
            return Stream.of(
                    Arguments.of("A".repeat(30), "30 символов (максимум для имени)"),
                    Arguments.of("B".repeat(30), "30 символов (максимум для фамилии)"),
                    Arguments.of("C".repeat(1000), "1000 символов (длинное bio)")
            );
        }

        @ParameterizedTest
        @MethodSource("provideLongTexts")
        @DisplayName("Работа с максимальными длинами полей")
        void handling_MaximumLengthFields_ShouldWorkCorrectly(String text, String description) {
            // given
            User user = createTestUser(EXISTING_USER_ID, "testuser");
            user.setFirstName(text);
            user.setLastName(text);
            user.setBio(text);

            when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(user));

            // when
            UserProfileResponse result = userService.getUserProfile(EXISTING_USER_ID);

            // then
            assertThat(result.getFirstName())
                    .as("Проверка firstName: " + description)
                    .hasSize(text.length());

            assertThat(result.getLastName())
                    .as("Проверка lastName: " + description)
                    .hasSize(text.length());

            assertThat(result.getBio())
                    .as("Проверка bio: " + description)
                    .hasSize(text.length());
        }
    }

    @Nested
    @DisplayName("Специальные сценарии")
    class SpecialScenarios {

        @Test
        @DisplayName("Обновление email тем же email должно сбросить верификацию")
        void updateEmail_WithSameEmail_ShouldResetVerification() {
            // given
            String sameEmail = "same@example.com";
            User user = createTestUser(EXISTING_USER_ID, "testUser");
            user.setEmail(sameEmail);
            user.setEmail_verified(true);

            when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(user));

            // when
            userService.updateEmail(EXISTING_USER_ID, sameEmail);

            // then
            assertThat(user.getEmail())
                    .as("Email должен остаться прежним")
                    .isEqualTo(sameEmail);

            assertThat(user.isEmail_verified())
                    .as("Верификация должна сброситься")
                    .isFalse();

            verify(userRepository).findById(EXISTING_USER_ID);
        }

        @Test
        @DisplayName("Замена существующего аватара на новый")
        void updateUserAvatar_WhenUserHasExistingAvatar_ShouldReplaceAvatar() {
            // given
            User user = createTestUser(EXISTING_USER_ID, "testUser");
            user.setAvatarUrl("http://example.com/old-avatar.jpg");
            MultipartFile file = mock(MultipartFile.class);
            String newAvatarUrl = "http://example.com/new-avatar.jpg";

            when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(user));
            when(file.isEmpty()).thenReturn(false);
            when(fileStorageService.saveUserAvatar(file, EXISTING_USER_ID)).thenReturn(newAvatarUrl);

            // when
            userService.updateUserAvatar(file, EXISTING_USER_ID);

            // then
            assertThat(user.getAvatarUrl())
                    .as("Аватар должен обновиться")
                    .isEqualTo(newAvatarUrl)
                    .isNotEqualTo("http://example.com/old-avatar.jpg");

            verify(fileStorageService).saveUserAvatar(file, EXISTING_USER_ID);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Пользователь с null полями должен корректно маппиться в response")
        void getUserProfile_WhenUserHasNullFields_ShouldReturnResponseWithNullFields() {
            // given
            User user = createTestUser(EXISTING_USER_ID, "testuser");
            user.setBio(null);
            user.setFirstName(null);
            user.setLastName(null);

            when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(user));

            // when
            UserProfileResponse result = userService.getUserProfile(EXISTING_USER_ID);

            // then
            assertThat(result)
                    .as("Ответ должен содержать null для соответствующих полей")
                    .extracting(
                            UserProfileResponse::getBio,
                            UserProfileResponse::getFirstName,
                            UserProfileResponse::getLastName
                    )
                    .containsExactly(null, null, null);
        }

        @Test
        @DisplayName("Проверка смены email_verified с false на true")
        void verifyUserEmail_ShouldChangeEmailVerifiedFromFalseToTrue() {
            // given
            User user = createTestUser(EXISTING_USER_ID, "testUser");
            user.setEmail_verified(false);
            when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(user));

            // when
            userService.verifyUserEmail(EXISTING_USER_ID);

            // then
            assertThat(user.isEmail_verified())
                    .as("Email должен стать верифицированным")
                    .isTrue();

            verify(userRepository).findById(EXISTING_USER_ID);
        }
    }

    @Nested
    @DisplayName("Проверки с особыми символами и форматами")
    class SpecialCharactersTests {

        @Test
        @DisplayName("Username с разными форматами должен находиться корректно")
        void findUsername_WithDifferentUsernameFormats_ShouldFindUser() {
            // given
            String[] usernames = {"user123", "User_Name", "user-name", "user.name"};
            User user = createTestUser(ANOTHER_USER_ID, "");

            for (String username : usernames) {
                user.setUsername(username);
                when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

                // when
                UserProfileResponse result = userService.findUsername(username, EXISTING_USER_ID);

                // then
                assertThat(result.getUsername())
                        .as("Должен найти пользователя с username: " + username)
                        .isEqualTo(username);
            }

            verify(userRepository, times(usernames.length)).findByUsername(anyString());
        }

        @Test
        @DisplayName("Пользователь со спецсимволами должен корректно отображаться")
        void getUserProfile_WithSpecialCharacters_ShouldReturnCorrectResponse() {
            // given
            User user = createTestUser(EXISTING_USER_ID, "test_user-123");
            user.setFirstName("John-Michael");
            user.setLastName("O'Conner");
            user.setBio("Разработчик 🚀 | Java & Spring | Москва");

            when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(user));

            // when
            UserProfileResponse result = userService.getUserProfile(EXISTING_USER_ID);

            // then
            assertThat(result)
                    .as("Спецсимволы должны сохраняться")
                    .extracting(
                            UserProfileResponse::getFirstName,
                            UserProfileResponse::getLastName,
                            UserProfileResponse::getUsername,
                            UserProfileResponse::getBio
                    )
                    .containsExactly(
                            "John-Michael",
                            "O'Conner",
                            "test_user-123",
                            "Разработчик 🚀 | Java & Spring | Москва"
                    );
        }
    }
}