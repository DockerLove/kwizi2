package com.example.kwizi.security;

import com.example.kwizi.exception.JwtAuthenticationException;
import com.example.kwizi.repository.RevokedTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtRequestFilterTest {

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private RevokedTokenRepository revokedTokenRepo;

    @InjectMocks
    private JwtRequestFilter jwtRequestFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @Test
    void doFilterInternal_ShouldAuthenticateUser_WhenValidTokenIsProvided() throws ServletException, IOException {
        // Arrange
        String jwt = "validJwtToken";
        String username = "testUser";
        String authorizationHeader = "Bearer " + jwt;
        UserDetails userDetails = org.springframework.security.core.userdetails.User.withUsername(username).password("password").roles("USER").build();

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

        // Ensure SecurityContextHolder is set
        assert SecurityContextHolder.getContext().getAuthentication() != null;
    }

    @Test
    void doFilterInternal_ShouldNotAuthenticateUser_WhenTokenIsRevoked() throws ServletException, IOException {
        // Arrange
        String jwt = "revokedJwtToken";
        String authorizationHeader = "Bearer " + jwt;
        String errorMessage = "Токен был отозван";

        when(request.getHeader("Authorization")).thenReturn(authorizationHeader);
        when(jwtUtils.extractJti(jwt)).thenReturn("jti");
        when(revokedTokenRepo.existsById("jti")).thenReturn(true);

        // Act
        jwtRequestFilter.doFilterInternal(request, response, chain);

        // Assert
        verify(request).getHeader("Authorization");
        verify(jwtUtils).extractJti(jwt);
        verify(revokedTokenRepo).existsById("jti");
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, errorMessage);
        verifyNoMoreInteractions(jwtUtils);  // Make sure there are NO MORE interactions with jwtUtils
        verifyNoInteractions(userDetailsService, chain);  // Make sure these are never called

    }

    @Test
    void doFilterInternal_ShouldSendError_WhenJwtAuthenticationExceptionOccurs() throws ServletException, IOException {
        // Arrange
        String jwt = "invalidJwtToken";
        String authorizationHeader = "Bearer " + jwt;
        String errorMessage = "Invalid token";

        when(request.getHeader("Authorization")).thenReturn(authorizationHeader);
        when(jwtUtils.extractJti(jwt)).thenThrow(new JwtAuthenticationException(errorMessage));

        // Act
        jwtRequestFilter.doFilterInternal(request, response, chain);

        // Assert
        verify(request).getHeader("Authorization");
        verify(jwtUtils).extractJti(jwt);  // Now verify that extractJti IS called
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, errorMessage); // Verify sendError with the correct message
        verifyNoInteractions(userDetailsService, revokedTokenRepo);  // Other methods should not be called
        verifyNoInteractions(chain);

    }

    @Test
    void doFilterInternal_ShouldContinueFilterChain_WhenAuthorizationHeaderIsMissing() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);

        // Act
        jwtRequestFilter.doFilterInternal(request, response, chain);

        // Assert
        verify(request).getHeader("Authorization");
        verifyNoInteractions(jwtUtils, userDetailsService, revokedTokenRepo);
        verify(chain).doFilter(request, response);

        // Ensure SecurityContextHolder is not set
        assert SecurityContextHolder.getContext().getAuthentication() == null;
    }

    @Test
    void doFilterInternal_ShouldContinueFilterChain_WhenAuthorizationHeaderDoesNotStartWithBearer() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Basic someToken");

        // Act
        jwtRequestFilter.doFilterInternal(request, response, chain);

        // Assert
        verify(request).getHeader("Authorization");
        verifyNoInteractions(jwtUtils, userDetailsService, revokedTokenRepo);
        verify(chain).doFilter(request, response);

        // Ensure SecurityContextHolder is not set
        assert SecurityContextHolder.getContext().getAuthentication() == null;
    }

    @Test
    void shouldNotFilter_ShouldReturnTrue_WhenRequestPathIsLogin() {
        when(request.getServletPath()).thenReturn("/api/auth/login");
        try {
            assert jwtRequestFilter.shouldNotFilter(request);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldNotFilter_ShouldReturnFalse_WhenRequestPathIsNotLogin() {
        when(request.getServletPath()).thenReturn("/api/resource");
        try {
            assert !jwtRequestFilter.shouldNotFilter(request);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
    }
}