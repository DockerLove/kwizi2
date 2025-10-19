package com.example.kwizi.security;

import com.example.kwizi.exception.JwtAuthenticationException;
import com.example.kwizi.repository.RevokedTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

    private final UserDetailsService userDetailsService;
    private final JwtUtils jwtUtils;
    private final RevokedTokenRepository revokedTokenRepo;
    private final JwtFilterExceptionHandler jwtFilterExceptionHandler;

    @Autowired
    public JwtRequestFilter(UserDetailsService userDetailsService,
                            JwtUtils jwtUtils,
                            RevokedTokenRepository revokedTokenRepo,JwtFilterExceptionHandler jwtFilterExceptionHandler) {
        this.userDetailsService = userDetailsService;
        this.jwtUtils = jwtUtils;
        this.revokedTokenRepo = revokedTokenRepo;
        this.jwtFilterExceptionHandler = jwtFilterExceptionHandler;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String jwtToken = extractJwtToken(request);

        if (jwtToken != null) {
            try {
                processJwtAuthentication(jwtToken, request);
            } catch (Exception e) {
                jwtFilterExceptionHandler.handleJwtException(e, response);
                return;
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return request.getServletPath().equals("/api/auth/login"); // Не фильтруем /api/auth/login
    }

    private String extractJwtToken(HttpServletRequest request) {
        final String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String jwt = authorizationHeader.substring(7);
            logger.debug("Получен JWT из заголовка: {}", jwt);
            return jwt;
        }

        return null;
    }

    private void processJwtAuthentication(String jwtToken, HttpServletRequest request) {
        validateTokenNotRevoked(jwtToken);

        String username = extractUsernameFromToken(jwtToken);

        if (shouldAuthenticateUser(username)) {
            authenticateUser(jwtToken, username, request);
        }
    }

    private void validateTokenNotRevoked(String jwtToken) {
        String jti = jwtUtils.extractJti(jwtToken);
        if (revokedTokenRepo.existsById(jti)) {
            logger.warn("Токен отозван: {}", jti);
            throw new JwtAuthenticationException("Токен был отозван");
        }
    }

    private String extractUsernameFromToken(String jwtToken) {
        String username = jwtUtils.getUsernameFromToken(jwtToken);
        logger.debug("Извлечено имя пользователя из JWT: {}", username);
        return username;
    }

    private boolean shouldAuthenticateUser(String username) {
        return username != null && SecurityContextHolder.getContext().getAuthentication() == null;
    }

    private void authenticateUser(String jwtToken, String username, HttpServletRequest request) {
        UserDetails userDetails = loadUserDetails(username);

        if (isTokenValid(jwtToken, userDetails)) {
            setAuthenticationInContext(userDetails, request);
            logger.info("Пользователь {} аутентифицирован с помощью JWT", username);
        } else {
            logger.warn("Не удалось проверить JWT для пользователя: {}", username);
        }
    }

    private UserDetails loadUserDetails(String username) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        logger.debug("Загружены детали пользователя для: {}", username);
        return userDetails;
    }

    private boolean isTokenValid(String jwtToken, UserDetails userDetails) {
        return jwtUtils.validateToken(jwtToken, userDetails);
    }

    private void setAuthenticationInContext(UserDetails userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
