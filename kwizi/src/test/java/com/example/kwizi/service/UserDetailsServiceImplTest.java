package com.example.kwizi.service;

import com.example.kwizi.model.User;
import com.example.kwizi.repository.AuthenticationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.OffsetDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private AuthenticationRepository authenticationRepository;

    @Mock
    private Environment environment;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User createTestUser(Long id){
        User mockUser = new User();
        mockUser.setId(id);
        mockUser.setEmail_verified(false);
        mockUser.setUsername("ivan_ivanov");
        mockUser.setEmail("ivan@example.com");
        mockUser.setPassword("encodedPassword");
        mockUser.setFirstName("Иван");
        mockUser.setLastName("Петров");
        mockUser.setBio("bio");
        mockUser.setCreatedAt(OffsetDateTime.now());
        return mockUser;
    }

    @Test
    void loadUserByUsername_WhenUserNotExists_ThrowsException() {
        // Arrange
        String nonExistentUsername = "nonExistentUser";
        when(authenticationRepository.findByUsername(nonExistentUsername))
                .thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(nonExistentUsername)
        );

        assertEquals("Пользователь с username: " + nonExistentUsername + " не найден!",
                exception.getMessage());
        verify(authenticationRepository, times(1)).findByUsername(nonExistentUsername);
    }

    @Test
    void loadUserByUsername_WhenUserExists_ReturnsUserDetails() {
        // Arrange
        Long id = 1L;
        User mockUser =createTestUser(id);

        when(authenticationRepository.findByUsername(mockUser.getUsername()))
                .thenReturn(Optional.of(mockUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(mockUser.getUsername());

        // Assert
        assertNotNull(userDetails);
        assertEquals(mockUser.getUsername(), userDetails.getUsername());
        verify(authenticationRepository, times(1)).findByUsername(mockUser.getUsername());
    }
}
