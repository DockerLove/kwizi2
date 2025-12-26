package com.example.kwizi.service;

import com.example.kwizi.model.User;
import com.example.kwizi.repository.AuthenticationRepository;
import com.example.kwizi.security.UserDetailsImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailsServiceImpl тесты")
class UserDetailsServiceImplTest {

    @Mock
    private AuthenticationRepository authenticationRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private final String EXISTING_USERNAME = "testuser";
    private final String NON_EXISTING_USERNAME = "nonexistinguser";
    private final Long USER_ID = 1L;

    @Nested
    @DisplayName("Основные сценарии")
    class MainScenarios {

        @Test
        @DisplayName("✅ Успешная загрузка пользователя по username")
        void loadUserByUsername_WithValidUsername_ShouldReturnUserDetails() {
            // given
            User user = createTestUser(USER_ID, EXISTING_USERNAME);
            user.setPassword("encodedPassword123");
            user.setEmail_verified(true);

            when(authenticationRepository.findByUsername(EXISTING_USERNAME))
                    .thenReturn(Optional.of(user));

            // when
            UserDetails userDetails = userDetailsService.loadUserByUsername(EXISTING_USERNAME);

            // then
            assertThat(userDetails)
                    .as("Должен вернуться UserDetails")
                    .isNotNull()
                    .isInstanceOf(UserDetailsImpl.class);

            assertThat(userDetails)
                    .extracting(
                            UserDetails::getUsername,
                            UserDetails::getPassword,
                            UserDetails::isEnabled
                    )
                    .containsExactly(
                            EXISTING_USERNAME,
                            "encodedPassword123",
                            true
                    );

            verify(authenticationRepository).findByUsername(EXISTING_USERNAME);
        }

        @Test
        @DisplayName("✅ Username с пробелами должен быть обрезан")
        void loadUserByUsername_WithUsernameWithSpaces_ShouldTrimUsername() {
            // given
            String usernameWithSpaces = "  testuser  ";
            User user = createTestUser(USER_ID, EXISTING_USERNAME);

            when(authenticationRepository.findByUsername(EXISTING_USERNAME))
                    .thenReturn(Optional.of(user));

            // when
            UserDetails userDetails = userDetailsService.loadUserByUsername(usernameWithSpaces);

            // then
            assertThat(userDetails)
                    .as("Должен найти пользователя после trim")
                    .extracting(UserDetails::getUsername)
                    .isEqualTo(EXISTING_USERNAME);

            verify(authenticationRepository).findByUsername(EXISTING_USERNAME);
        }
    }

    @Nested
    @DisplayName("Сценарии с ошибками")
    class ErrorScenarios {

        @Test
        @DisplayName("❌ Пользователь не найден - выбрасывает UsernameNotFoundException")
        void loadUserByUsername_WhenUserNotFound_ShouldThrowUsernameNotFoundException() {
            // given
            when(authenticationRepository.findByUsername(NON_EXISTING_USERNAME))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername(NON_EXISTING_USERNAME))
                    .as("Должно выбросить исключение для несуществующего пользователя")
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("Пользователь '%s' не найден", NON_EXISTING_USERNAME);

            verify(authenticationRepository).findByUsername(NON_EXISTING_USERNAME);
        }
    }

    @Nested
    @DisplayName("Параметризованные тесты для некорректных username")
    class ParameterizedInvalidUsernameTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "  ", "\t", "\n"})
        @DisplayName("❌ Пустые и null username выбрасывают исключение")
        void loadUserByUsername_WithInvalidUsernames_ShouldThrowIllegalArgumentException(String invalidUsername) {
            // when & then
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername(invalidUsername))
                    .as("Должно выбросить исключение для некорректного username: '%s'", invalidUsername)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Username пуст или равен null");

            verify(authenticationRepository, never()).findByUsername(anyString());
        }

        @ParameterizedTest
        @ValueSource(strings = {"  user  ", "user ", " user", "\tuser\t"})
        @DisplayName("Username с пробелами должен корректно обрезаться")
        void loadUserByUsername_WithTrimmedUsernames_ShouldFindUser(String trimmedUsername) {
            // given
            User user = createTestUser(USER_ID, "user");

            when(authenticationRepository.findByUsername("user"))
                    .thenReturn(Optional.of(user));

            // when
            UserDetails userDetails = userDetailsService.loadUserByUsername(trimmedUsername);

            // then
            assertThat(userDetails)
                    .as("Должен найти пользователя после trim")
                    .isNotNull();

            verify(authenticationRepository).findByUsername("user");
        }
    }

    @Nested
    @DisplayName("Проверка возвращаемого типа и структуры")
    class ReturnTypeAndStructureTests {

        @Test
        @DisplayName("✅ Возвращает UserDetailsImpl с правильными полями")
        void loadUserByUsername_ShouldReturnUserDetailsImpl() {
            // given
            User user = createTestUser(USER_ID, EXISTING_USERNAME);
            user.setPassword("$2a$10$encodedPasswordHash");
            user.setEmail_verified(true);

            when(authenticationRepository.findByUsername(EXISTING_USERNAME))
                    .thenReturn(Optional.of(user));

            // when
            UserDetails userDetails = userDetailsService.loadUserByUsername(EXISTING_USERNAME);

            // then
            assertThat(userDetails)
                    .as("Должен вернуть экземпляр UserDetailsImpl")
                    .isInstanceOf(UserDetailsImpl.class);

            UserDetailsImpl detailsImpl = (UserDetailsImpl) userDetails;
            assertThat(detailsImpl)
                    .extracting(
                            UserDetailsImpl::getUsername,
                            UserDetailsImpl::getPassword,
                            UserDetailsImpl::isEnabled
                    )
                    .containsExactly(
                            EXISTING_USERNAME,
                            "$2a$10$encodedPasswordHash",
                            true
                    );

            verify(authenticationRepository).findByUsername(EXISTING_USERNAME);
        }

        @Test
        @DisplayName("✅ Проверка authorities пользователя")
        void loadUserByUsername_ShouldReturnUserWithAuthorities() {
            // given
            User user = createTestUser(USER_ID, EXISTING_USERNAME);
            user.setEmail_verified(true);

            when(authenticationRepository.findByUsername(EXISTING_USERNAME))
                    .thenReturn(Optional.of(user));

            // when
            UserDetails userDetails = userDetailsService.loadUserByUsername(EXISTING_USERNAME);

            // then
            assertThat(userDetails.getAuthorities())
                    .as("Должен иметь непустые authorities")
                    .isNotNull();

            verify(authenticationRepository).findByUsername(EXISTING_USERNAME);
        }

    }

    private User createTestUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail("test@example.com");
        user.setPassword("password");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail_verified(true);
        return user;
    }
}