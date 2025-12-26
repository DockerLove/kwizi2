package com.example.kwizi.security;

import com.example.kwizi.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;

import java.lang.reflect.Field;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("UserDetailsImpl тесты")
@ExtendWith(MockitoExtension.class)
class UserDetailsImplTest {

    @Mock
    private User mockUser;

    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(1L);
        when(mockUser.getUsername()).thenReturn("testuser");
        when(mockUser.getPassword()).thenReturn("password123");
        when(mockUser.getEmail()).thenReturn("test@example.com");
        userDetails = new UserDetailsImpl(mockUser);
    }

    @Nested
    @DisplayName("Основные сценарии")
    class MainScenarios {

        @Test
        @DisplayName("✅ Создание экземпляра с пользователем")
        void constructor_ShouldCreateInstanceWithUser() {
            assertThat(userDetails).isNotNull();
        }

        @Test
        @DisplayName("✅ Получение пустых полномочий")
        void getAuthorities_ShouldReturnEmptyCollection() {
            Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
            assertThat(authorities).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("✅ Получение пароля")
        void getPassword_ShouldReturnUserPassword() {
            String password = userDetails.getPassword();
            assertThat(password).isEqualTo("password123");
            verify(mockUser).getPassword();
        }

        @Test
        @DisplayName("✅ Получение имени пользователя")
        void getUsername_ShouldReturnUsername() {
            String username = userDetails.getUsername();
            assertThat(username).isEqualTo("testuser");
            verify(mockUser).getUsername();
        }

        @Test
        @DisplayName("✅ Аккаунт не истёк")
        void isAccountNonExpired_ShouldAlwaysReturnTrue() {
            assertThat(userDetails.isAccountNonExpired()).isTrue();
        }

        @Test
        @DisplayName("✅ Аккаунт не заблокирован")
        void isAccountNonLocked_ShouldAlwaysReturnTrue() {
            assertThat(userDetails.isAccountNonLocked()).isTrue();
        }

        @Test
        @DisplayName("✅ Учётные данные не истекли")
        void isCredentialsNonExpired_ShouldAlwaysReturnTrue() {
            assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        }

        @Test
        @DisplayName("✅ Аккаунт включён")
        void isEnabled_ShouldAlwaysReturnTrue() {
            assertThat(userDetails.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("✅ Получение ID пользователя")
        void getId_ShouldReturnUserId() {
            Long id = userDetails.getId();
            assertThat(id).isEqualTo(1L);
            verify(mockUser).getId();
        }

        @Test
        @DisplayName("✅ Получение email пользователя")
        void getEmail_ShouldReturnUserEmail() {
            String email = userDetails.getEmail();
            assertThat(email).isEqualTo("test@example.com");
            verify(mockUser).getEmail();
        }

        @Test
        @DisplayName("✅ Повторный вызов getAuthorities возвращает пустую коллекцию")
        void getAuthorities_ShouldReturnSameInstanceMultipleTimes() {
            Collection<? extends GrantedAuthority> authorities1 = userDetails.getAuthorities();
            Collection<? extends GrantedAuthority> authorities2 = userDetails.getAuthorities();
            assertThat(authorities1).isNotNull().isEmpty();
            assertThat(authorities2).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("✅ Внутренний пользователь совпадает с переданным")
        void getUser_ShouldReturnCorrectUser() throws Exception {
            Field userField = UserDetailsImpl.class.getDeclaredField("user");
            userField.setAccessible(true);
            User retrievedUser = (User) userField.get(userDetails);
            assertThat(retrievedUser).isSameAs(mockUser);
        }

        @Test
        @DisplayName("✅ Объект неизменяем")
        void shouldBeImmutable() {
            User anotherUser = mock(User.class);
            when(anotherUser.getUsername()).thenReturn("anotheruser");
            assertThat(userDetails.getUsername()).isEqualTo("testuser");
            assertThat(userDetails.getUsername()).isNotEqualTo("anotheruser");
        }
    }
}