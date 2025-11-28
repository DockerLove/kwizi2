package com.example.kwizi.service;

import com.example.kwizi.DTO.response.UserProfileResponse;
import com.example.kwizi.exception.BusinessLogicException;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.UserRepository;
import com.example.kwizi.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
    private final String OLD_USERNAME = "oldUser";
    private final String NEW_USERNAME = "newUser";
    private final String NEW_TOKEN = "new.jwt.token";

    @Test
    void updateUsername_WithValidNewUsername_ShouldUpdateAndReturnToken() {
        // Arrange
        User existingUser = createTestUser(EXISTING_USER_ID, OLD_USERNAME);

        when(userRepository.existsByUsername(NEW_USERNAME)).thenReturn(false);
        when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.of(existingUser));
        when(jwtUtils.generateToken(NEW_USERNAME)).thenReturn(NEW_TOKEN);

        // Act
        String resultToken = userService.updateUsername(EXISTING_USER_ID, NEW_USERNAME);

        // Assert
        assertThat(resultToken).isEqualTo(NEW_TOKEN);
        assertThat(existingUser.getUsername()).isEqualTo(NEW_USERNAME);
        verify(userRepository).existsByUsername(NEW_USERNAME);
        verify(userRepository).findById(EXISTING_USER_ID);
        verify(jwtUtils).generateToken(NEW_USERNAME);
    }

    @Test
    void updateUsername_WhenUsernameAlreadyTaken_ShouldThrowException() {
        // Arrange
        when(userRepository.existsByUsername(NEW_USERNAME)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUsername(EXISTING_USER_ID, NEW_USERNAME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Имя пользователя занято");

        verify(userRepository).existsByUsername(NEW_USERNAME);
        verify(userRepository, never()).findById(anyLong());
        verify(jwtUtils, never()).generateToken(anyString());
    }

    @Test
    void updateUsername_WhenUserNotFound_ShouldThrowUserNotFoundException() {
        // Arrange
        when(userRepository.existsByUsername(NEW_USERNAME)).thenReturn(false);
        when(userRepository.findById(EXISTING_USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUsername(EXISTING_USER_ID, NEW_USERNAME))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("Пользователь не найден");

        verify(userRepository).existsByUsername(NEW_USERNAME);
        verify(userRepository).findById(EXISTING_USER_ID);
        verify(jwtUtils, never()).generateToken(anyString());
    }

    @Test
    void verifyUserEmail_WhenEmailNotVerified_ShouldVerifyEmail() {
        // Arrange
        Long userId = 1L;
        User user = createTestUser(userId, "testUser");
        user.setEmail_verified(false); // Явно указываем, что email не верифицирован

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        userService.verifyUserEmail(userId);

        // Assert
        assertThat(user.isEmail_verified()).isTrue();
        verify(userRepository).findById(userId);
        // Проверяем, что пользователь сохранился (из-за @Transactional)
    }

    @Test
    void verifyUserEmail_WhenEmailAlreadyVerified_ShouldThrowException() {
        // Arrange
        Long userId = 1L;
        User user = createTestUser(userId, "testUser");
        user.setEmail_verified(true); // Email уже верифицирован

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() -> userService.verifyUserEmail(userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email уже подтвержден");

        verify(userRepository).findById(userId);
        // Email должен остаться верифицированным
        assertThat(user.isEmail_verified()).isTrue();
    }

    @Test
    void verifyUserEmail_WhenUserNotFound_ShouldThrowUserNotFoundException() {
        // Arrange
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.verifyUserEmail(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("Пользователь не найден");

        verify(userRepository).findById(userId);
    }

    @Test
    void verifyUserEmail_ShouldChangeEmailVerifiedFromFalseToTrue() {
        // Arrange
        Long userId = 1L;
        User user = createTestUser(userId, "testUser");
        user.setEmail_verified(false); // Начальное состояние

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        userService.verifyUserEmail(userId);

        // Assert
        // Проверяем, что состояние изменилось
        assertThat(user.isEmail_verified())
                .as("Email должен быть верифицирован после вызова метода")
                .isTrue();
    }

    @Test
    void updateBio_WithValidBio_ShouldUpdateUserBio() {
        // Arrange
        Long userId = 1L;
        String newBio = "Новое био пользователя";
        User user = createTestUser(userId, "testUser");
        user.setBio("Старое био"); // начальное значение

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        userService.updateBio(userId, newBio);

        // Assert
        assertThat(user.getBio()).isEqualTo(newBio);
        verify(userRepository).findById(userId);
    }

    @Test
    void updateBio_WithEmptyBio_ShouldUpdateUserBioToEmpty() {
        // Arrange
        Long userId = 1L;
        String emptyBio = "";
        User user = createTestUser(userId, "testUser");
        user.setBio("Старое био");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        userService.updateBio(userId, emptyBio);

        // Assert
        assertThat(user.getBio()).isEmpty();
        verify(userRepository).findById(userId);
    }

    @Test
    void updateBio_WithNullBio_ShouldUpdateUserBioToNull() {
        // Arrange
        Long userId = 1L;
        User user = createTestUser(userId, "testUser");
        user.setBio("Старое био");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        userService.updateBio(userId, null);

        // Assert
        assertThat(user.getBio()).isNull();
        verify(userRepository).findById(userId);
    }

    @Test
    void updateBio_WhenUserNotFound_ShouldThrowUserNotFoundException() {
        // Arrange
        Long userId = 999L;
        String newBio = "Новое био";

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.updateBio(userId, newBio))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("Пользователь не найден");

        verify(userRepository).findById(userId);
    }
    @Test
    void updateBio_WithLongBio_ShouldUpdateSuccessfully() {
        // Arrange
        Long userId = 1L;
        // Создаем действительно длинное био (1000 символов)
        String longBio = "A".repeat(1000);
        User user = createTestUser(userId, "testUser");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        userService.updateBio(userId, longBio);

        // Assert
        assertThat(user.getBio()).isEqualTo(longBio);
        assertThat(user.getBio()).hasSize(1000); // Теперь точно 1000 символов
        verify(userRepository).findById(userId);
    }

    @Test
    void updateFirstName_WithValidName_ShouldUpdateFirstName() {
        // Arrange
        Long userId = 1L;
        String newFirstName = "Александр";
        User user = createTestUser(userId, "testUser");
        user.setFirstName("СтароеИмя");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        userService.updateFirstName(userId, newFirstName);

        // Assert
        assertThat(user.getFirstName()).isEqualTo(newFirstName);
        verify(userRepository).findById(userId);
    }

    @Test
    void updateFirstName_WithMinimumLengthName_ShouldUpdateSuccessfully() {
        // Arrange
        Long userId = 1L;
        String minLengthName = "Ан"; // 2 символа - минимальная длина
        User user = createTestUser(userId, "testUser");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        userService.updateFirstName(userId, minLengthName);

        // Assert
        assertThat(user.getFirstName()).isEqualTo(minLengthName);
        assertThat(user.getFirstName()).hasSize(2);
        verify(userRepository).findById(userId);
    }

    @Test
    void updateFirstName_WithMaximumLengthName_ShouldUpdateSuccessfully() {
        // Arrange
        Long userId = 1L;
        String maxLengthName = "А".repeat(30); // 30 символов - максимальная длина
        User user = createTestUser(userId, "testUser");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        userService.updateFirstName(userId, maxLengthName);

        // Assert
        assertThat(user.getFirstName()).isEqualTo(maxLengthName);
        assertThat(user.getFirstName()).hasSize(30);
        verify(userRepository).findById(userId);
    }

    @Test
    void updateFirstName_WithDifferentValidNames_ShouldUpdateSuccessfully() {
        // Arrange
        Long userId = 1L;
        User user = createTestUser(userId, "testUser");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act & Assert для разных валидных имен
        String[] validNames = {"John", "Mary-Jane", "Анна-Мария", "Jean Paul", "О'Коннор"};

        for (String name : validNames) {
            userService.updateFirstName(userId, name);
            assertThat(user.getFirstName()).isEqualTo(name);
        }

        verify(userRepository, times(validNames.length)).findById(userId);
    }

    @Test
    void updateFirstName_WhenUserNotFound_ShouldThrowUserNotFoundException() {
        // Arrange
        Long userId = 999L;
        String newFirstName = "НовоеИмя";

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.updateFirstName(userId, newFirstName))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("Пользователь не найден");

        verify(userRepository).findById(userId);
    }

    @Test
    void updateFirstName_WithNullName_ShouldUpdateToNull() {
        // Arrange
        Long userId = 1L;
        User user = createTestUser(userId, "testUser");
        user.setFirstName("СтароеИмя");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        userService.updateFirstName(userId, null);

        // Assert
        assertThat(user.getFirstName()).isNull();
        verify(userRepository).findById(userId);
    }

    @Test
    void updateFirstName_WithEmptyName_ShouldUpdateToEmpty() {
        // Arrange
        Long userId = 1L;
        String emptyName = "";
        User user = createTestUser(userId, "testUser");
        user.setFirstName("СтароеИмя");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        userService.updateFirstName(userId, emptyName);

        // Assert
        assertThat(user.getFirstName()).isEmpty();
        verify(userRepository).findById(userId);
    }

    @Test
    void updateLastName_WithValidLastName_ShouldUpdateLastName() {
        // Arrange
        Long userId = 1L;
        String newLastName = "Иванов";
        User user = createTestUser(userId, "testUser");
        user.setLastName("СтараяФамилия");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        userService.updateLastName(userId, newLastName);

        // Assert
        assertThat(user.getLastName()).isEqualTo(newLastName);
        verify(userRepository).findById(userId);
    }

    @Test
    void updateLastName_WithMinimumLengthName_ShouldUpdateSuccessfully() {
        // Arrange
        Long userId = 1L;
        String minLengthLastName = "Ли"; // 2 символа - минимальная длина
        User user = createTestUser(userId, "testUser");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        userService.updateLastName(userId, minLengthLastName);

        // Assert
        assertThat(user.getLastName()).isEqualTo(minLengthLastName);
        assertThat(user.getLastName()).hasSize(2);
        verify(userRepository).findById(userId);
    }

    @Test
    void updateLastName_WithMaximumLengthName_ShouldUpdateSuccessfully() {
        // Arrange
        Long userId = 1L;
        String maxLengthLastName = "Ф".repeat(30); // 30 символов - максимальная длина
        User user = createTestUser(userId, "testUser");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        userService.updateLastName(userId, maxLengthLastName);

        // Assert
        assertThat(user.getLastName()).isEqualTo(maxLengthLastName);
        assertThat(user.getLastName()).hasSize(30);
        verify(userRepository).findById(userId);
    }

    @Test
    void updateLastName_WithDifferentValidLastNames_ShouldUpdateSuccessfully() {
        // Arrange
        Long userId = 1L;
        User user = createTestUser(userId, "testUser");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act & Assert для разных валидных фамилий
        String[] validLastNames = {"Smith", "Johnson-Williams", "Петров-Водкин", "O'Brien", "ван Дер Варт"};

        for (String lastName : validLastNames) {
            userService.updateLastName(userId, lastName);
            assertThat(user.getLastName()).isEqualTo(lastName);
        }

        verify(userRepository, times(validLastNames.length)).findById(userId);
    }

    @Test
    void updateLastName_WhenUserNotFound_ShouldThrowUserNotFoundException() {
        // Arrange
        Long userId = 999L;
        String newLastName = "НоваяФамилия";

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.updateLastName(userId, newLastName))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("Пользователь не найден");

        verify(userRepository).findById(userId);
    }

    @Test
    void updateLastName_WithNullLastName_ShouldUpdateToNull() {
        // Arrange
        Long userId = 1L;
        User user = createTestUser(userId, "testUser");
        user.setLastName("СтараяФамилия");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        userService.updateLastName(userId, null);

        // Assert
        assertThat(user.getLastName()).isNull();
        verify(userRepository).findById(userId);
    }

    @Test
    void updateLastName_WithEmptyLastName_ShouldUpdateToEmpty() {
        // Arrange
        Long userId = 1L;
        String emptyLastName = "";
        User user = createTestUser(userId, "testUser");
        user.setLastName("СтараяФамилия");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        userService.updateLastName(userId, emptyLastName);

        // Assert
        assertThat(user.getLastName()).isEmpty();
        verify(userRepository).findById(userId);
    }

    @Test
    void updateEmail_WithValidEmail_ShouldUpdateEmailAndResetVerification() {
        // Arrange
        Long userId = 1L;
        String newEmail = "newemail@example.com";
        User user = createTestUser(userId, "testUser");
        user.setEmail("old@example.com");
        user.setEmail_verified(true); // был верифицирован

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        userService.updateEmail(userId, newEmail);

        // Assert
        assertThat(user.getEmail()).isEqualTo(newEmail);
        assertThat(user.isEmail_verified()).isFalse(); // должен сброситься
        verify(userRepository).findById(userId);
    }

    @Test
    void updateEmail_WithDifferentValidFormats_ShouldUpdateSuccessfully() {
        // Arrange
        Long userId = 1L;
        User user = createTestUser(userId, "testUser");
        user.setEmail_verified(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act & Assert для разных валидных email
        String[] validEmails = {
                "simple@example.com",
                "user.name@example.com",
                "user_name@example.com",
                "user+tag@example.com",
                "user@sub.domain.com",
                "user@example.co.uk"
        };

        for (String email : validEmails) {
            user.setEmail_verified(true); // сбрасываем перед каждым тестом
            userService.updateEmail(userId, email);
            assertThat(user.getEmail()).isEqualTo(email);
            assertThat(user.isEmail_verified()).isFalse();
        }

        verify(userRepository, times(validEmails.length)).findById(userId);
    }

    @Test
    void updateEmail_WhenUserNotFound_ShouldThrowUserNotFoundException() {
        // Arrange
        Long userId = 999L;
        String newEmail = "new@example.com";

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.updateEmail(userId, newEmail))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("Пользователь не найден");

        verify(userRepository).findById(userId);
    }

    @Test
    void updateEmail_WithNullEmail_ShouldUpdateToNullAndResetVerification() {
        // Arrange
        Long userId = 1L;
        User user = createTestUser(userId, "testUser");
        user.setEmail("old@example.com");
        user.setEmail_verified(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        userService.updateEmail(userId, null);

        // Assert
        assertThat(user.getEmail()).isNull();
        assertThat(user.isEmail_verified()).isFalse();
        verify(userRepository).findById(userId);
    }

    @Test
    void updateEmail_WithEmptyEmail_ShouldUpdateToEmptyAndResetVerification() {
        // Arrange
        Long userId = 1L;
        String emptyEmail = "";
        User user = createTestUser(userId, "testUser");
        user.setEmail("old@example.com");
        user.setEmail_verified(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        userService.updateEmail(userId, emptyEmail);

        // Assert
        assertThat(user.getEmail()).isEmpty();
        assertThat(user.isEmail_verified()).isFalse();
        verify(userRepository).findById(userId);
    }

    @Test
    void updateEmail_WithSameEmail_ShouldResetVerification() {
        // Arrange
        Long userId = 1L;
        String sameEmail = "same@example.com";
        User user = createTestUser(userId, "testUser");
        user.setEmail(sameEmail);
        user.setEmail_verified(true); // был верифицирован

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        userService.updateEmail(userId, sameEmail);

        // Assert
        assertThat(user.getEmail()).isEqualTo(sameEmail); // email не изменился
        assertThat(user.isEmail_verified()).isFalse(); // но верификация сбросилась
        verify(userRepository).findById(userId);
    }

    @Test
    void updateEmail_WhenEmailWasNotVerified_ShouldKeepNotVerified() {
        // Arrange
        Long userId = 1L;
        String newEmail = "new@example.com";
        User user = createTestUser(userId, "testUser");
        user.setEmail_verified(false); // уже не верифицирован

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        userService.updateEmail(userId, newEmail);

        // Assert
        assertThat(user.getEmail()).isEqualTo(newEmail);
        assertThat(user.isEmail_verified()).isFalse(); // остается не верифицированным
        verify(userRepository).findById(userId);
    }

    @Test
    void updateChatAvatar_WithValidFile_ShouldUpdateAvatarUrl() {
        // Arrange
        Long userId = 1L;
        User user = createTestUser(userId, "testUser");
        MultipartFile file = mock(MultipartFile.class);
        String avatarUrl = "http://example.com/avatars/user1.jpg";

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(file.isEmpty()).thenReturn(false);
        when(fileStorageService.saveUserAvatar(file, userId)).thenReturn(avatarUrl);

        // Act
        userService.updateUserAvatar(file, userId);

        // Assert
        assertThat(user.getAvatarUrl()).isEqualTo(avatarUrl);
        verify(userRepository).findById(userId);
        verify(fileStorageService).saveUserAvatar(file, userId);
        verify(userRepository).save(user);
    }

    @Test
    void updateChatAvatar_WithEmptyFile_ShouldThrowBusinessLogicException() {
        // Arrange
        Long userId = 1L;
        User user = createTestUser(userId, "testUser");
        MultipartFile file = mock(MultipartFile.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(file.isEmpty()).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUserAvatar(file, userId))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessage("Файл не может быть пустым");

        verify(userRepository).findById(userId);
        verify(fileStorageService, never()).saveUserAvatar(any(), anyLong());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateChatAvatar_WhenUserNotFound_ShouldThrowUserNotFoundException() {
        // Arrange
        Long userId = 999L;
        MultipartFile file = mock(MultipartFile.class);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUserAvatar(file, userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("Пользователь не найден");

        verify(userRepository).findById(userId);
        verify(fileStorageService, never()).saveUserAvatar(any(), anyLong());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateChatAvatar_WhenUserHasExistingAvatar_ShouldReplaceAvatar() {
        // Arrange
        Long userId = 1L;
        User user = createTestUser(userId, "testUser");
        user.setAvatarUrl("http://example.com/old-avatar.jpg"); // старый аватар

        MultipartFile file = mock(MultipartFile.class);
        String newAvatarUrl = "http://example.com/new-avatar.jpg";

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(file.isEmpty()).thenReturn(false);
        when(fileStorageService.saveUserAvatar(file, userId)).thenReturn(newAvatarUrl);

        // Act
        userService.updateUserAvatar(file, userId);

        // Assert
        assertThat(user.getAvatarUrl()).isEqualTo(newAvatarUrl);
        assertThat(user.getAvatarUrl()).isNotEqualTo("http://example.com/old-avatar.jpg");
        verify(fileStorageService).saveUserAvatar(file, userId);
        verify(userRepository).save(user);
    }

    @Test
    void updateChatAvatar_WhenUserHasNoAvatar_ShouldSetNewAvatar() {
        // Arrange
        Long userId = 1L;
        User user = createTestUser(userId, "testUser");
        user.setAvatarUrl(null); // аватара не было

        MultipartFile file = mock(MultipartFile.class);
        String newAvatarUrl = "http://example.com/first-avatar.jpg";

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(file.isEmpty()).thenReturn(false);
        when(fileStorageService.saveUserAvatar(file, userId)).thenReturn(newAvatarUrl);

        // Act
        userService.updateUserAvatar(file, userId);

        // Assert
        assertThat(user.getAvatarUrl()).isEqualTo(newAvatarUrl);
        verify(fileStorageService).saveUserAvatar(file, userId);
        verify(userRepository).save(user);
    }

    @Test
    void updateChatAvatar_WhenFileStorageFails_ShouldPropagateException() {
        // Arrange
        Long userId = 1L;
        User user = createTestUser(userId, "testUser");
        MultipartFile file = mock(MultipartFile.class);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(file.isEmpty()).thenReturn(false);
        when(fileStorageService.saveUserAvatar(file, userId))
                .thenThrow(new BusinessLogicException("Ошибка сохранения файла"));

        // Act & Assert
        assertThatThrownBy(() -> userService.updateUserAvatar(file, userId))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessage("Ошибка сохранения файла");

        verify(userRepository).findById(userId);
        verify(fileStorageService).saveUserAvatar(file, userId);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateChatAvatar_ShouldSaveCorrectAvatarUrl() {
        // Arrange
        Long userId = 1L;
        User user = createTestUser(userId, "testUser");
        MultipartFile file = mock(MultipartFile.class);
        String expectedAvatarUrl = "avatars/user_1_profile.jpg";

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(file.isEmpty()).thenReturn(false);
        when(fileStorageService.saveUserAvatar(file, userId)).thenReturn(expectedAvatarUrl);

        // Act
        userService.updateUserAvatar(file, userId);

        // Assert
        assertThat(user.getAvatarUrl())
                .isEqualTo(expectedAvatarUrl)
                .isNotNull()
                .isNotBlank();
        verify(userRepository).save(user);
    }

    @Test
    void findUsername_WithDifferentUser_ShouldReturnUserProfile() {
        // Arrange
        Long requesterId = 1L;
        Long targetUserId = 2L;
        String username = "targetUser";

        User targetUser = createTestUser(targetUserId, username);
        targetUser.setFirstName("John");
        targetUser.setLastName("Doe");
        targetUser.setBio("Test bio");
        targetUser.setEmail("target@example.com");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(targetUser));

        // Act
        UserProfileResponse result = userService.findUsername(username, requesterId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(targetUserId);
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        assertThat(result.getBio()).isEqualTo("Test bio");
        assertThat(result.getEmail()).isEqualTo("target@example.com");
        verify(userRepository).findByUsername(username);
    }

    @Test
    void findUsername_WhenSearchingSelf_ShouldThrowException() {
        // Arrange
        Long requesterId = 1L;
        String username = "myUsername";

        User selfUser = createTestUser(requesterId, username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(selfUser));

        // Act & Assert
        assertThatThrownBy(() -> userService.findUsername(username, requesterId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Нельзя искать самого себя");

        verify(userRepository).findByUsername(username);
    }

    @Test
    void findUsername_WhenUserNotFound_ShouldThrowUserNotFoundException() {
        // Arrange
        Long requesterId = 1L;
        String username = "nonExistingUser";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.findUsername(username, requesterId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("Пользователь не найден");

        verify(userRepository).findByUsername(username);
    }

    @Test
    void findUsername_WithDifferentUsernameFormats_ShouldFindUser() {
        // Arrange
        Long requesterId = 1L;
        Long targetUserId = 2L;

        String[] usernames = {"user123", "User_Name", "user-name", "user.name"};

        for (String username : usernames) {
            User targetUser = createTestUser(targetUserId, username);
            when(userRepository.findByUsername(username)).thenReturn(Optional.of(targetUser));

            // Act
            UserProfileResponse result = userService.findUsername(username, requesterId);

            // Assert
            assertThat(result.getUsername()).isEqualTo(username);
        }

        verify(userRepository, times(usernames.length)).findByUsername(anyString());
    }

    @Test
    void findUsername_ShouldCorrectlyMapAllFieldsToResponse() {
        // Arrange
        Long requesterId = 1L;
        Long targetUserId = 2L;
        String username = "testuser";

        User targetUser = createTestUser(targetUserId, username);
        targetUser.setFirstName("Иван");
        targetUser.setLastName("Петров");
        targetUser.setBio("Разработчик из Москвы");
        targetUser.setEmail("ivan@example.com");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(targetUser));

        // Act
        UserProfileResponse result = userService.findUsername(username, requesterId);

        // Assert
        assertThat(result)
                .extracting(
                        UserProfileResponse::getId,
                        UserProfileResponse::getFirstName,
                        UserProfileResponse::getLastName,
                        UserProfileResponse::getUsername,
                        UserProfileResponse::getBio,
                        UserProfileResponse::getEmail
                )
                .containsExactly(
                        targetUserId,
                        "Иван",
                        "Петров",
                        username,
                        "Разработчик из Москвы",
                        "ivan@example.com"
                );
    }

    @Test
    void getUserProfile_WithValidUser_ShouldReturnUserProfileResponse() {
        // Arrange
        Long userId = 1L;
        User user = createTestUser(userId, "testuser");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setBio("Software Developer");
        user.setEmail("john@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        UserProfileResponse result = userService.getUserProfile(userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getBio()).isEqualTo("Software Developer");
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        verify(userRepository).findById(userId);
    }

    @Test
    void getUserProfile_WhenUserNotFound_ShouldThrowUserNotFoundException() {
        // Arrange
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getUserProfile(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("Пользователь не найден");

        verify(userRepository).findById(userId);
    }

    @Test
    void getUserProfile_WhenUserHasNullFields_ShouldReturnResponseWithNullFields() {
        // Arrange
        Long userId = 1L;
        User user = createTestUser(userId, "testuser");
        user.setBio(null); // bio is null

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        UserProfileResponse result = userService.getUserProfile(userId);

        // Assert
        assertThat(result.getBio()).isNull();
        assertThat(result.getFirstName()).isNotNull(); // обязательные поля не null
        assertThat(result.getLastName()).isNotNull();
        assertThat(result.getUsername()).isNotNull();
        assertThat(result.getEmail()).isNotNull();
    }

    @Test
    void getUserProfile_WhenUserHasEmptyFields_ShouldReturnResponseWithEmptyFields() {
        // Arrange
        Long userId = 1L;
        User user = createTestUser(userId, "testuser");
        user.setBio(""); // empty bio
        user.setFirstName(""); // empty first name
        user.setLastName(""); // empty last name

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        UserProfileResponse result = userService.getUserProfile(userId);

        // Assert
        assertThat(result.getBio()).isEmpty();
        assertThat(result.getFirstName()).isEmpty();
        assertThat(result.getLastName()).isEmpty();
        assertThat(result.getUsername()).isEqualTo("testuser"); // username остается
        assertThat(result.getEmail()).isNotNull(); // email остается
    }

    @Test
    void getUserProfile_ShouldCorrectlyMapAllUserFieldsToResponse() {
        // Arrange
        Long userId = 1L;
        User user = createTestUser(userId, "johndoe");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setBio("Senior Developer with 5 years experience");
        user.setEmail("john.doe@company.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        UserProfileResponse result = userService.getUserProfile(userId);

        // Assert
        assertThat(result)
                .extracting(
                        UserProfileResponse::getId,
                        UserProfileResponse::getFirstName,
                        UserProfileResponse::getLastName,
                        UserProfileResponse::getUsername,
                        UserProfileResponse::getBio,
                        UserProfileResponse::getEmail
                )
                .containsExactly(
                        userId,
                        "John",
                        "Doe",
                        "johndoe",
                        "Senior Developer with 5 years experience",
                        "john.doe@company.com"
                );
    }

    @Test
    void getUserProfile_WithMaximumLengthFields_ShouldReturnCorrectResponse() {
        // Arrange
        Long userId = 1L;
        User user = createTestUser(userId, "testuser");
        user.setFirstName("A".repeat(30)); // максимальная длина имени
        user.setLastName("B".repeat(30));  // максимальная длина фамилии
        user.setBio("C".repeat(1000));     // длинное bio

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        UserProfileResponse result = userService.getUserProfile(userId);

        // Assert
        assertThat(result.getFirstName()).hasSize(30);
        assertThat(result.getLastName()).hasSize(30);
        assertThat(result.getBio()).hasSize(1000);
        assertThat(result.getFirstName()).isEqualTo("A".repeat(30));
        assertThat(result.getLastName()).isEqualTo("B".repeat(30));
    }

    @Test
    void getUserProfile_WithSpecialCharacters_ShouldReturnCorrectResponse() {
        // Arrange
        Long userId = 1L;
        User user = createTestUser(userId, "test_user-123");
        user.setFirstName("John-Michael");
        user.setLastName("O'Conner");
        user.setBio("Разработчик 🚀 | Java & Spring | Москва");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        UserProfileResponse result = userService.getUserProfile(userId);

        // Assert
        assertThat(result.getFirstName()).isEqualTo("John-Michael");
        assertThat(result.getLastName()).isEqualTo("O'Conner");
        assertThat(result.getUsername()).isEqualTo("test_user-123");
        assertThat(result.getBio()).isEqualTo("Разработчик 🚀 | Java & Spring | Москва");
    }


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
}