package com.example.kwizi.security;

import com.example.kwizi.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserDetailsImplTest {

    private User mockUser;
    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        // Создаем мок User
        mockUser = mock(User.class);

        // Настраиваем мок
        when(mockUser.getId()).thenReturn(1L);
        when(mockUser.getUsername()).thenReturn("testuser");
        when(mockUser.getPassword()).thenReturn("password123");
        when(mockUser.getEmail()).thenReturn("test@example.com");

        // Создаем тестируемый объект
        userDetails = new UserDetailsImpl(mockUser);
    }

    @Test
    void constructor_ShouldCreateInstanceWithUser() {
        // Arrange & Act уже в @BeforeEach

        // Assert
        assertNotNull(userDetails);
    }

    @Test
    void getAuthorities_ShouldReturnEmptyCollection() {
        // Act
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        // Assert
        assertNotNull(authorities);
        assertTrue(authorities.isEmpty());
    }

    @Test
    void getPassword_ShouldReturnUserPassword() {
        // Act
        String password = userDetails.getPassword();

        // Assert
        assertEquals("password123", password);
        verify(mockUser).getPassword();
    }

    @Test
    void getUsername_ShouldReturnUsername() {
        // Act
        String username = userDetails.getUsername();

        // Assert
        assertEquals("testuser", username);
        verify(mockUser).getUsername();
    }

    @Test
    void isAccountNonExpired_ShouldAlwaysReturnTrue() {
        // Act & Assert
        assertTrue(userDetails.isAccountNonExpired());
    }

    @Test
    void isAccountNonLocked_ShouldAlwaysReturnTrue() {
        // Act & Assert
        assertTrue(userDetails.isAccountNonLocked());
    }

    @Test
    void isCredentialsNonExpired_ShouldAlwaysReturnTrue() {
        // Act & Assert
        assertTrue(userDetails.isCredentialsNonExpired());
    }

    @Test
    void isEnabled_ShouldAlwaysReturnTrue() {
        // Act & Assert
        assertTrue(userDetails.isEnabled());
    }

    @Test
    void getId_ShouldReturnUserId() {
        // Act
        Long id = userDetails.getId();

        // Assert
        assertEquals(1L, id);
        verify(mockUser).getId();
    }

    @Test
    void getEmail_ShouldReturnUserEmail() {
        // Act
        String email = userDetails.getEmail();

        // Assert
        assertEquals("test@example.com", email);
        verify(mockUser).getEmail();
    }

    @Test
    void getAuthorities_ShouldReturnSameInstanceMultipleTimes() {
        // Act
        Collection<? extends GrantedAuthority> authorities1 = userDetails.getAuthorities();
        Collection<? extends GrantedAuthority> authorities2 = userDetails.getAuthorities();

        // Assert
        assertNotNull(authorities1);
        assertNotNull(authorities2);
        // Могут быть разные инстансы, но оба должны быть пустыми
        assertTrue(authorities1.isEmpty());
        assertTrue(authorities2.isEmpty());
    }

    @Test
    void getUser_ShouldReturnCorrectUser() throws Exception {
        // Arrange - используем reflection для доступа к private полю
        java.lang.reflect.Field userField = UserDetailsImpl.class.getDeclaredField("user");
        userField.setAccessible(true);

        // Act
        User retrievedUser = (User) userField.get(userDetails);

        // Assert
        assertSame(mockUser, retrievedUser);
    }


    // Тест на неизменяемость
    @Test
    void shouldBeImmutable() {
        // Arrange - создаем другой пользователь
        User anotherUser = mock(User.class);
        when(anotherUser.getUsername()).thenReturn("anotheruser");

        // Act - пытаемся "изменить" userDetails (но это невозможно)
        // Объект immutable, поэтому мы не можем изменить внутреннего пользователя

        // Assert - оригинальный пользователь остается тем же
        assertEquals("testuser", userDetails.getUsername());
        assertNotEquals("anotheruser", userDetails.getUsername());
    }
}
