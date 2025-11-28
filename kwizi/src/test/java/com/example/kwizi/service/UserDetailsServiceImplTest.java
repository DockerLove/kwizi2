package com.example.kwizi.service;

import com.example.kwizi.model.User;
import com.example.kwizi.repository.AuthenticationRepository;
import com.example.kwizi.security.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class UserDetailsServiceImplTest {

    @Mock
    private AuthenticationRepository authenticationRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private final String EXISTING_USERNAME = "testuser";
    private final String NON_EXISTING_USERNAME = "nonexistinguser";

    @Test
    void loadUserByUsername_WithValidUsername_ShouldReturnUserDetails() {
        // Arrange
        User user = createTestUser(1L, EXISTING_USERNAME);
        when(authenticationRepository.findByUsername(EXISTING_USERNAME))
                .thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(EXISTING_USERNAME);

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(EXISTING_USERNAME);
        assertThat(userDetails.getPassword()).isEqualTo(user.getPassword());
        verify(authenticationRepository).findByUsername(EXISTING_USERNAME);
    }

    @Test
    void loadUserByUsername_WhenUserNotFound_ShouldThrowUsernameNotFoundException() {
        // Arrange
        when(authenticationRepository.findByUsername(NON_EXISTING_USERNAME))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(NON_EXISTING_USERNAME))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Пользователь '%s' не найден", NON_EXISTING_USERNAME);

        verify(authenticationRepository).findByUsername(NON_EXISTING_USERNAME);
    }

    @Test
    void loadUserByUsername_WithNullUsername_ShouldThrowIllegalArgumentException() {
        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username пуст или равен null");

        verify(authenticationRepository, never()).findByUsername(anyString());
    }

    @Test
    void loadUserByUsername_WithEmptyUsername_ShouldThrowIllegalArgumentException() {
        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username пуст или равен null");

        verify(authenticationRepository, never()).findByUsername(anyString());
    }

    @Test
    void loadUserByUsername_WithUsernameWithSpaces_ShouldTrimUsername() {
        // Arrange
        String usernameWithSpaces = "  testuser  ";
        User user = createTestUser(1L, EXISTING_USERNAME);

        when(authenticationRepository.findByUsername(EXISTING_USERNAME))
                .thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(usernameWithSpaces);

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(EXISTING_USERNAME);
        verify(authenticationRepository).findByUsername(EXISTING_USERNAME);
    }

    @Test
    void loadUserByUsername_ShouldReturnUserDetailsImpl() {
        // Arrange
        User user = createTestUser(1L, EXISTING_USERNAME);
        user.setPassword("encodedPassword");
        user.setEmail_verified(true);

        when(authenticationRepository.findByUsername(EXISTING_USERNAME))
                .thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(EXISTING_USERNAME);

        // Assert
        assertThat(userDetails).isInstanceOf(UserDetailsImpl.class);
        assertThat(userDetails.getUsername()).isEqualTo(EXISTING_USERNAME);
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword");
        assertThat(userDetails.isEnabled()).isTrue();
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