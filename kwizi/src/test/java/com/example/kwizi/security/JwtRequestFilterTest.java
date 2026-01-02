package com.example.kwizi.security;

import com.example.kwizi.exception.JwtAuthenticationException;
import com.example.kwizi.repository.RevokedTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@DisplayName("JwtRequestFilter тесты")
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
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Основные сценарии фильтрации")
    class MainFilterScenarios {

        @Test
        @DisplayName("Аутентифицирует пользователя при валидном токене")
        void doFilterInternal_ShouldAuthenticateUser_WhenValidTokenIsProvided() throws ServletException, IOException {
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

            jwtRequestFilter.doFilterInternal(request, response, chain);

            verify(request).getHeader("Authorization");
            verify(jwtUtils).extractJti(jwt);
            verify(revokedTokenRepo).existsById("jti");
            verify(jwtUtils).getUsernameFromToken(jwt);
            verify(userDetailsService).loadUserByUsername(username);
            verify(jwtUtils).validateToken(jwt, userDetails);
            verify(chain).doFilter(request, response);
            verifyNoInteractions(jwtFilterExceptionHandler);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getName()).isEqualTo(username);
        }

        @Test
        @DisplayName("Продолжает цепочку фильтров, если заголовок Authorization отсутствует")
        void doFilterInternal_ShouldContinueFilterChain_WhenAuthorizationHeaderIsMissing() throws ServletException, IOException {
            when(request.getHeader("Authorization")).thenReturn(null);

            jwtRequestFilter.doFilterInternal(request, response, chain);

            verify(request).getHeader("Authorization");
            verifyNoInteractions(jwtUtils, userDetailsService, revokedTokenRepo, jwtFilterExceptionHandler);
            verify(chain).doFilter(request, response);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Продолжает цепочку фильтров, если заголовок не начинается с Bearer")
        void doFilterInternal_ShouldContinueFilterChain_WhenAuthorizationHeaderDoesNotStartWithBearer() throws ServletException, IOException {
            when(request.getHeader("Authorization")).thenReturn("Basic someToken");

            jwtRequestFilter.doFilterInternal(request, response, chain);

            verify(request).getHeader("Authorization");
            verifyNoInteractions(jwtUtils, userDetailsService, revokedTokenRepo, jwtFilterExceptionHandler);
            verify(chain).doFilter(request, response);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Продолжает цепочку фильтров, если пользователь уже аутентифицирован")
        void doFilterInternal_ShouldContinueFilterChain_WhenUserAlreadyAuthenticated() throws ServletException, IOException {
            String jwt = "validJwtToken";
            String username = "testUser";
            String authorizationHeader = "Bearer " + jwt;

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

            jwtRequestFilter.doFilterInternal(request, response, chain);

            verify(request).getHeader("Authorization");
            verify(jwtUtils).extractJti(jwt);
            verify(revokedTokenRepo).existsById("jti");
            verify(jwtUtils).getUsernameFromToken(jwt);
            verifyNoInteractions(userDetailsService);
            verify(chain).doFilter(request, response);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getName()).isEqualTo("alreadyAuthenticated");
        }

        @Test
        @DisplayName("Не устанавливает аутентификацию при невалидном токене")
        void doFilterInternal_ShouldHandleException_WhenTokenValidationFails() throws ServletException, IOException {
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
            when(jwtUtils.validateToken(jwt, userDetails)).thenReturn(false);

            jwtRequestFilter.doFilterInternal(request, response, chain);

            verify(request).getHeader("Authorization");
            verify(jwtUtils).extractJti(jwt);
            verify(revokedTokenRepo).existsById("jti");
            verify(jwtUtils).getUsernameFromToken(jwt);
            verify(userDetailsService).loadUserByUsername(username);
            verify(jwtUtils).validateToken(jwt, userDetails);
            verify(chain).doFilter(request, response);
            verifyNoInteractions(jwtFilterExceptionHandler);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("Обработка ошибок")
    class ExceptionHandlingScenarios {

        @Test
        @DisplayName("Обрабатывает исключение при отозванном токене")
        void doFilterInternal_ShouldHandleException_WhenTokenIsRevoked() throws ServletException, IOException {
            String jwt = "revokedJwtToken";
            String authorizationHeader = "Bearer " + jwt;

            when(request.getHeader("Authorization")).thenReturn(authorizationHeader);
            when(jwtUtils.extractJti(jwt)).thenReturn("jti");
            when(revokedTokenRepo.existsById("jti")).thenReturn(true);
            doNothing().when(jwtFilterExceptionHandler).handleJwtException(any(Exception.class), eq(response));

            jwtRequestFilter.doFilterInternal(request, response, chain);

            verify(request).getHeader("Authorization");
            verify(jwtUtils).extractJti(jwt);
            verify(revokedTokenRepo).existsById("jti");
            verify(jwtFilterExceptionHandler).handleJwtException(any(JwtAuthenticationException.class), eq(response));
            verifyNoMoreInteractions(jwtUtils);
            verifyNoInteractions(userDetailsService, chain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Обрабатывает исключение приJwtAuthenticationException")
        void doFilterInternal_ShouldHandleException_WhenJwtAuthenticationExceptionOccurs() throws ServletException, IOException {
            String jwt = "invalidJwtToken";
            String authorizationHeader = "Bearer " + jwt;
            String errorMessage = "Invalid token";

            when(request.getHeader("Authorization")).thenReturn(authorizationHeader);
            when(jwtUtils.extractJti(jwt)).thenThrow(new JwtAuthenticationException(errorMessage));
            doNothing().when(jwtFilterExceptionHandler).handleJwtException(any(Exception.class), eq(response));

            jwtRequestFilter.doFilterInternal(request, response, chain);

            verify(request).getHeader("Authorization");
            verify(jwtUtils).extractJti(jwt);
            verify(jwtFilterExceptionHandler).handleJwtException(any(JwtAuthenticationException.class), eq(response));
            verifyNoInteractions(userDetailsService, revokedTokenRepo, chain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("Исключение путей из фильтрации")
    class ShouldNotFilterTests {

        @Test
        @DisplayName("Пропускает фильтрацию для /api/auth/login")
        void shouldNotFilter_ShouldReturnTrue_WhenRequestPathIsLogin() {
            when(request.getServletPath()).thenReturn("/api/auth/login");
            assertThat(jwtRequestFilter.shouldNotFilter(request)).isTrue();
            verify(request).getServletPath();
        }

        @Test
        @DisplayName("Применяет фильтрацию для других путей")
        void shouldNotFilter_ShouldReturnFalse_WhenRequestPathIsNotLogin() {
            when(request.getServletPath()).thenReturn("/api/resource");
            assertThat(jwtRequestFilter.shouldNotFilter(request)).isFalse();
            verify(request).getServletPath();
        }
    }
}