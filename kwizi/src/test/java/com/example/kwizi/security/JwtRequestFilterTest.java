package com.example.kwizi.security;

import com.example.kwizi.exception.JwtAuthenticationException;
import com.example.kwizi.repository.RevokedTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtRequestFilterTest {

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private RevokedTokenRepository revokedTokenRepo;

    @Mock
    private JwtFilterExceptionHandler jwtFilterExceptionHandler;

    @InjectMocks
    private JwtRequestFilter jwtRequestFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        // Очищаем SecurityContext перед каждым тестом
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        // Очищаем SecurityContext после каждого теста
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_ShouldAuthenticateUser_WhenValidTokenIsProvided() throws ServletException, IOException {
        // Arrange
        String jwt = "validJwtToken";
        String username = "testUser";
        String authorizationHeader = "Bearer " + jwt;
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password("password")
                .roles("USER")
                .build();

        when(request.getHeader("Authorization")).thenReturn(authorizationHeader);
        when(jwtUtils.extractJti(jwt)).thenReturn("jti");
        when(revokedTokenRepo.existsById("jti")).thenReturn(false);
        when(jwtUtils.getUsernameFromToken(jwt)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtUtils.validateToken(jwt, userDetails)).thenReturn(true);

        // Act
        jwtRequestFilter.doFilterInternal(request, response, chain);

        // Assert
        verify(request).getHeader("Authorization");
        verify(jwtUtils).extractJti(jwt);
        verify(revokedTokenRepo).existsById("jti");
        verify(jwtUtils).getUsernameFromToken(jwt);
        verify(userDetailsService).loadUserByUsername(username);
        verify(jwtUtils).validateToken(jwt, userDetails);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtFilterExceptionHandler); // Не должно вызываться исключение

        // Проверяем что SecurityContextHolder установлен
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(username, SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void doFilterInternal_ShouldHandleException_WhenTokenIsRevoked() throws ServletException, IOException {
        // Arrange
        String jwt = "revokedJwtToken";
        String authorizationHeader = "Bearer " + jwt;

        when(request.getHeader("Authorization")).thenReturn(authorizationHeader);
        when(jwtUtils.extractJti(jwt)).thenReturn("jti");
        when(revokedTokenRepo.existsById("jti")).thenReturn(true);

        // Мокаем обработчик исключений
        doNothing().when(jwtFilterExceptionHandler).handleJwtException(any(Exception.class), eq(response));

        // Act
        jwtRequestFilter.doFilterInternal(request, response, chain);

        // Assert
        verify(request).getHeader("Authorization");
        verify(jwtUtils).extractJti(jwt);
        verify(revokedTokenRepo).existsById("jti");
        verify(jwtFilterExceptionHandler).handleJwtException(any(JwtAuthenticationException.class), eq(response));
        verifyNoMoreInteractions(jwtUtils);
        verifyNoInteractions(userDetailsService, chain);

        // Проверяем что SecurityContextHolder не установлен
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_ShouldHandleException_WhenJwtAuthenticationExceptionOccurs() throws ServletException, IOException {
        // Arrange
        String jwt = "invalidJwtToken";
        String authorizationHeader = "Bearer " + jwt;
        String errorMessage = "Invalid token";

        when(request.getHeader("Authorization")).thenReturn(authorizationHeader);
        when(jwtUtils.extractJti(jwt)).thenThrow(new JwtAuthenticationException(errorMessage));

        // Мокаем обработчик исключений
        doNothing().when(jwtFilterExceptionHandler).handleJwtException(any(Exception.class), eq(response));

        // Act
        jwtRequestFilter.doFilterInternal(request, response, chain);

        // Assert
        verify(request).getHeader("Authorization");
        verify(jwtUtils).extractJti(jwt);
        verify(jwtFilterExceptionHandler).handleJwtException(any(JwtAuthenticationException.class), eq(response));
        verifyNoInteractions(userDetailsService, revokedTokenRepo, chain);

        // Проверяем что SecurityContextHolder не установлен
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_ShouldHandleException_WhenTokenValidationFails() throws ServletException, IOException {
        // Arrange
        String jwt = "invalidJwtToken";
        String username = "testUser";
        String authorizationHeader = "Bearer " + jwt;
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password("password")
                .roles("USER")
                .build();

        when(request.getHeader("Authorization")).thenReturn(authorizationHeader);
        when(jwtUtils.extractJti(jwt)).thenReturn("jti");
        when(revokedTokenRepo.existsById("jti")).thenReturn(false);
        when(jwtUtils.getUsernameFromToken(jwt)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtUtils.validateToken(jwt, userDetails)).thenReturn(false); // Токен невалиден

        // Act
        jwtRequestFilter.doFilterInternal(request, response, chain);

        // Assert
        verify(request).getHeader("Authorization");
        verify(jwtUtils).extractJti(jwt);
        verify(revokedTokenRepo).existsById("jti");
        verify(jwtUtils).getUsernameFromToken(jwt);
        verify(userDetailsService).loadUserByUsername(username);
        verify(jwtUtils).validateToken(jwt, userDetails);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtFilterExceptionHandler); // Не должно вызываться исключение

        // Проверяем что SecurityContextHolder не установлен
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_ShouldContinueFilterChain_WhenAuthorizationHeaderIsMissing() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);

        // Act
        jwtRequestFilter.doFilterInternal(request, response, chain);

        // Assert
        verify(request).getHeader("Authorization");
        verifyNoInteractions(jwtUtils, userDetailsService, revokedTokenRepo, jwtFilterExceptionHandler);
        verify(chain).doFilter(request, response);

        // Проверяем что SecurityContextHolder не установлен
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_ShouldContinueFilterChain_WhenAuthorizationHeaderDoesNotStartWithBearer() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Basic someToken");

        // Act
        jwtRequestFilter.doFilterInternal(request, response, chain);

        // Assert
        verify(request).getHeader("Authorization");
        verifyNoInteractions(jwtUtils, userDetailsService, revokedTokenRepo, jwtFilterExceptionHandler);
        verify(chain).doFilter(request, response);

        // Проверяем что SecurityContextHolder не установлен
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_ShouldContinueFilterChain_WhenUserAlreadyAuthenticated() throws ServletException, IOException {
        // Arrange
        String jwt = "validJwtToken";
        String username = "testUser";
        String authorizationHeader = "Bearer " + jwt;

        // Устанавливаем уже аутентифицированного пользователя
        UserDetails existingUserDetails = org.springframework.security.core.userdetails.User
                .withUsername("alreadyAuthenticated")
                .password("password")
                .roles("USER")
                .build();

        UsernamePasswordAuthenticationToken existingAuth =
                new UsernamePasswordAuthenticationToken(existingUserDetails, null, existingUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(request.getHeader("Authorization")).thenReturn(authorizationHeader);
        when(jwtUtils.extractJti(jwt)).thenReturn("jti");
        when(revokedTokenRepo.existsById("jti")).thenReturn(false);
        when(jwtUtils.getUsernameFromToken(jwt)).thenReturn(username);
        // userDetailsService.loadUserByUsername НЕ должен вызываться

        jwtRequestFilter.doFilterInternal(request, response, chain);

        // Assert
        verify(request).getHeader("Authorization");
        verify(jwtUtils).extractJti(jwt);
        verify(revokedTokenRepo).existsById("jti");
        verify(jwtUtils).getUsernameFromToken(jwt);
        verifyNoInteractions(userDetailsService);
        verify(chain).doFilter(request, response);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("alreadyAuthenticated", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void shouldNotFilter_ShouldReturnTrue_WhenRequestPathIsLogin() throws ServletException {
        when(request.getServletPath()).thenReturn("/api/auth/login");

        assertTrue(jwtRequestFilter.shouldNotFilter(request));
        verify(request).getServletPath();
    }

    @Test
    void shouldNotFilter_ShouldReturnFalse_WhenRequestPathIsNotLogin() throws ServletException {

        when(request.getServletPath()).thenReturn("/api/resource");

        assertFalse(jwtRequestFilter.shouldNotFilter(request));
        verify(request).getServletPath();
    }
}