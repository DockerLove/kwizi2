package com.example.kwizi.controller;

import com.example.kwizi.DTO.request.RegistrationRequest;
import com.example.kwizi.exception.JwtAuthenticationException;
import com.example.kwizi.model.RevokedToken;
import com.example.kwizi.model.User;
import com.example.kwizi.repository.RevokedTokenRepository;
import com.example.kwizi.security.JwtUtils;
import com.example.kwizi.service.AuthenticationService;
import com.example.kwizi.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private RegistrationService registrationService;
    @Mock
    private RevokedTokenRepository revokedTokenRepo;
    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private AuthenticationController authenticationController;

    @Test
    void registerUser_ShouldReturnCreated_WhenValidRequest() {
        // Arrange
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("testuser");
        request.setPassword("password");
        request.setEmail("test@example.com");
        BindingResult bindingResult = new StubBindingResult(request);

        // Act
        ResponseEntity<?> response = authenticationController.registerUser(request, bindingResult);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(registrationService).registerUser(request);
    }

    @Test
    void registerUser_ShouldReturnBadRequest_WhenValidationFails() {
        // Arrange
        RegistrationRequest request = new RegistrationRequest();
        BindingResult bindingResult = new StubBindingResult(request);
        bindingResult.rejectValue("username", "error.username", "Username is required");

        // Act
        ResponseEntity<?> response = authenticationController.registerUser(request, bindingResult);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof List);
        verifyNoInteractions(registrationService);
    }

    @Test
    void registerUser_ShouldReturnConflict_WhenUsernameAlreadyExists() {
        // Arrange
        RegistrationRequest registrationRequest = new RegistrationRequest();
        registrationRequest.setUsername("testUser");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        doThrow(new IllegalStateException("Username already exists")).when(registrationService).registerUser(registrationRequest);

        // Act
        ResponseEntity<?> response = authenticationController.registerUser(registrationRequest, bindingResult);

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Username already exists", response.getBody());
    }

    @Test
    void registerUser_ShouldReturnInternalServerError_WhenOtherException() {
        // Arrange
        RegistrationRequest registrationRequest = new RegistrationRequest();
        registrationRequest.setUsername("testUser");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        doThrow(new RuntimeException("Some error")).when(registrationService).registerUser(registrationRequest);

        // Act
        ResponseEntity<?> response = authenticationController.registerUser(registrationRequest, bindingResult);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Произошла ошибка при регистрации пользователя", response.getBody());
    }
    // Остальные тесты остаются без изменений...

    // Вспомогательный класс для реализации BindingResult
    private static class StubBindingResult extends BeanPropertyBindingResult {
        public StubBindingResult(Object target) {
            super(target, "target");
        }


        // Можно переопределить другие методы по необходимости
    }

    @Test
    void login_ShouldReturnToken_WhenCredentialsValid() {
        // Arrange
        AuthenticationRequest request = new AuthenticationRequest("user", "pass");
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("user");
        when(userDetailsService.loadUserByUsername("user")).thenReturn(userDetails);
        when(jwtUtils.generateToken("user")).thenReturn("token");

        // Act
        ResponseEntity<?> response = authenticationController.login(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("token", ((AuthenticationResponse)response.getBody()).getJwt());
    }

    @Test
    void login_ShouldReturnUnauthorized_WhenBadCredentials() {
        // Arrange
        AuthenticationRequest request = new AuthenticationRequest("user", "wrongpass");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act
        ResponseEntity<?> response = authenticationController.login(request);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Incorrect username or password", response.getBody());
    }


    @Test
    void login_ShouldReturnInternalServerError_WhenOtherException() {
        // Arrange
        AuthenticationRequest authenticationRequest = new AuthenticationRequest("testUser", "password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Some internal error"));

        // Act
        ResponseEntity<?> response = authenticationController.login(authenticationRequest);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Internal server error", response.getBody());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verifyNoInteractions(userDetailsService); // Ensure these aren't called on error
        verifyNoInteractions(jwtUtils);
    }

    @Test
    void logout_ShouldReturnNoContent_WhenInvalidToken() {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(jwtUtils.extractToken(request)).thenThrow(new JwtAuthenticationException("Invalid token"));

        // Act
        ResponseEntity<Void> response = authenticationController.logout(request);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verifyNoInteractions(revokedTokenRepo);
    }

    @Test
    void logout_ShouldReturnNoContent_WhenGeneralExceptionOccurs() {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        String token = "testToken";

        when(jwtUtils.extractToken(request)).thenReturn(token); // First set extractToken to return token
        when(jwtUtils.extractJti(token)).thenThrow(new RuntimeException("Some error")); // Then set extractJti to throw exception.

        // Act
        ResponseEntity<Void> response = authenticationController.logout(request);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(jwtUtils).extractToken(request);  // Make sure extractToken was called.
        verify(jwtUtils).extractJti(token); // Make sure extractJti was called
        verifyNoMoreInteractions(authenticationService); // Then verify that other methods were NOT called after exception.
        verifyNoMoreInteractions(revokedTokenRepo);
        verifyNoMoreInteractions(jwtUtils);
    }

    @Test
    void logout_ShouldReturnNoContent_WhenValidToken() {
        // Arrange
        User user = new User();
        user.setId(1L);

        when(jwtUtils.extractToken(request)).thenReturn("valid.token");
        when(jwtUtils.extractJti("valid.token")).thenReturn("jti123");
        when(jwtUtils.extractExpiration("valid.token")).thenReturn(new Date());
        when(jwtUtils.getUsernameFromToken("valid.token")).thenReturn("testuser");
        when(authenticationService.findByUsername("testuser")).thenReturn(Optional.of(user));

        // Mock the RevokedToken object that should be saved
        RevokedToken expectedToken = new RevokedToken();
        expectedToken.setJti("jti123");
        expectedToken.setUserId(1L);
        expectedToken.setUsername("testuser");
        expectedToken.setExpiresAt(any(Date.class));

        // Act
        ResponseEntity<Void> response = authenticationController.logout(request);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        // Verify the token was saved with correct parameters
        ArgumentCaptor<RevokedToken> tokenCaptor = ArgumentCaptor.forClass(RevokedToken.class);
        verify(revokedTokenRepo).save(tokenCaptor.capture());

        RevokedToken savedToken = tokenCaptor.getValue();
        assertEquals("jti123", savedToken.getJti());
        assertEquals(1L, savedToken.getUserId());
        assertEquals("testuser", savedToken.getUsername());
        assertNotNull(savedToken.getExpiresAt());
    }

    @Test
    void logout_ShouldHandleMissingToken() {
        // Arrange
        when(jwtUtils.extractToken(request)).thenReturn(null);

        // Act
        ResponseEntity<Void> response = authenticationController.logout(request);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verifyNoInteractions(revokedTokenRepo);
    }

    @Test
    void logout_ShouldHandleUserNotFound() {
        // Arrange
        when(jwtUtils.extractToken(request)).thenReturn("valid.token");
        when(jwtUtils.extractJti("valid.token")).thenReturn("jti123");
        when(jwtUtils.extractExpiration("valid.token")).thenReturn(new Date());
        when(jwtUtils.getUsernameFromToken("valid.token")).thenReturn("nonexistent");
        when(authenticationService.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Void> response = authenticationController.logout(request);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verifyNoInteractions(revokedTokenRepo);
    }

    @Test
    void logout_ShouldHandleJwtException() {
        // Arrange
        when(jwtUtils.extractToken(request))
                .thenThrow(new JwtAuthenticationException("Invalid token"));

        // Act
        ResponseEntity<Void> response = authenticationController.logout(request);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verifyNoInteractions(revokedTokenRepo);
    }

    @Test
    void logout_ShouldHandleGeneralException() {
        // Arrange
        when(jwtUtils.extractToken(request))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act
        ResponseEntity<Void> response = authenticationController.logout(request);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verifyNoInteractions(revokedTokenRepo);
    }
}