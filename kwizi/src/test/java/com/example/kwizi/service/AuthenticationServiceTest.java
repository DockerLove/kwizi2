package com.example.kwizi.service;

import com.example.kwizi.DTO.request.ChangePasswordRequest;
import com.example.kwizi.exception.JwtAuthenticationException;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.AuthenticationRepository;
import com.example.kwizi.security.JwtEmailVerify;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTest {

    @Mock
    private AuthenticationRepository authenticationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtEmailVerify jwtEmailVerify;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthenticationService authenticationService;


    @Test
    void sendVerificationEmail_ShouldSendEmail_WhenEmailNotVerified() {
        User user = new User();
        user.setId(1L);
        user.setEmail_verified(false);

        when(authenticationRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtEmailVerify.generateVerificationToken(1L)).thenReturn("token123");

        authenticationService.sendVerificationEmail(1L);

        verify(emailService).sendVerificationEmailAsync(any(), any());
    }

    @Test
    void sendVerificationEmail_ShouldThrow_WhenEmailAlreadyVerified() {
        User user = new User();
        user.setId(1L);
        user.setEmail_verified(true);

        when(authenticationRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () -> authenticationService.sendVerificationEmail(1L));
    }

    @Test
    void sendVerificationEmail_ShouldThrow_WhenUserNotFound() {
        when(authenticationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> authenticationService.sendVerificationEmail(1L));
    }

    @Test
    void verifyEmail_ShouldVerifyUserEmail_WhenTokenValid() {
        when(jwtEmailVerify.isTokenExpired("validToken")).thenReturn(false);
        when(jwtEmailVerify.getUserIdFromToken("validToken")).thenReturn("1");

        User user = new User();
        user.setId(1L);
        user.setEmail_verified(false);

        when(authenticationRepository.findById(1L)).thenReturn(Optional.of(user));

        authenticationService.verifyEmail("validToken");

        assertTrue(user.isEmail_verified());
        verify(authenticationRepository).save(user);
    }

    @Test
    void verifyEmail_ShouldThrow_WhenTokenExpired() {
        when(jwtEmailVerify.isTokenExpired("expiredToken")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authenticationService.verifyEmail("expiredToken"));
    }

    @Test
    void verifyEmail_ShouldThrow_WhenTokenInvalid() {
        when(jwtEmailVerify.isTokenExpired("invalidToken")).thenReturn(false);
        when(jwtEmailVerify.getUserIdFromToken("invalidToken")).thenThrow(new JwtAuthenticationException("Invalid token"));

        assertThrows(IllegalArgumentException.class, () -> authenticationService.verifyEmail("invalidToken"));
    }

    @Test
    void changePassword_ShouldUpdatePassword_WhenOldPasswordCorrect() {
        User user = new User();
        user.setUsername("user1");
        user.setPassword("oldEncodedPass");

        ChangePasswordRequest request = new ChangePasswordRequest( );
        request.setOldPassword("oldPass");
        request.setNewPassword("newPass");

        when(authenticationRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass", "oldEncodedPass")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("newEncodedPass");

        authenticationService.changePassword("user1", request);

        assertEquals("newEncodedPass", user.getPassword());
        verify(authenticationRepository).save(user);
    }

    @Test
    void changePassword_ShouldThrow_WhenOldPasswordIncorrect() {
        User user = new User();
        user.setUsername("user1");
        user.setPassword("oldEncodedPass");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("wrongPass");
        request.setNewPassword("newPass");

        when(authenticationRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", "oldEncodedPass")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authenticationService.changePassword("user1", request));
    }

    @Test
    void changePassword_ShouldThrow_WhenUserNotFound() {
        when(authenticationRepository.findByUsername("user1")).thenReturn(Optional.empty());
        ChangePasswordRequest request = new ChangePasswordRequest( );
        request.setOldPassword("oldPass");
        request.setNewPassword("newPass");
        assertThrows(UsernameNotFoundException.class, () ->
                authenticationService.changePassword("user1", request));
    }

    @Test
    void registerUser_ShouldEncodePasswordAndSaveUser() {
        User user = new User();
        user.setUsername("newUser");
        user.setPassword("rawPass");

        when(passwordEncoder.encode("rawPass")).thenReturn("encodedPass");
        when(authenticationRepository.save(user)).thenReturn(user);

        User savedUser = authenticationService.registerUser(user);

        assertEquals("encodedPass", savedUser.getPassword());
        verify(authenticationRepository).save(user);
    }

    @Test
    void findByUsername_ShouldReturnUser_WhenExists() {
        User user = new User();
        user.setUsername("existingUser");

        when(authenticationRepository.findByUsername("existingUser")).thenReturn(Optional.of(user));

        Optional<User> foundUser = authenticationService.findByUsername("existingUser");

        assertTrue(foundUser.isPresent());
        assertEquals("existingUser", foundUser.get().getUsername());
    }

    @Test
    void findByUsername_ShouldReturnEmpty_WhenNotExists() {
        when(authenticationRepository.findByUsername("nonExistingUser")).thenReturn(Optional.empty());

        Optional<User> foundUser = authenticationService.findByUsername("nonExistingUser");

        assertTrue(foundUser.isEmpty());
    }
}