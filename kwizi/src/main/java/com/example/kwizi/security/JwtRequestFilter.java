package com.example.kwizi.security;

import com.example.kwizi.exception.JwtAuthenticationException;
import com.example.kwizi.repository.RevokedTokenRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
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

    @Autowired
    public JwtRequestFilter(UserDetailsService userDetailsService,
                            JwtUtils jwtUtils,
                            RevokedTokenRepository revokedTokenRepo) {
        this.userDetailsService = userDetailsService;
        this.jwtUtils = jwtUtils;
        this.revokedTokenRepo = revokedTokenRepo;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String jwt = authorizationHeader.substring(7);
            logger.debug("Получен JWT из заголовка: {}", jwt); // Логируем получение JWT

            try {
                // Проверяем, не отозван ли токен
                String jti = jwtUtils.extractJti(jwt);
                if (revokedTokenRepo.existsById(jti)) {
                    logger.warn("Токен отозван: {}", jti);
                    throw new JwtAuthenticationException("Токен был отозван");
                }

                String username = jwtUtils.getUsernameFromToken(jwt);
                logger.debug("Извлечено имя пользователя из JWT: {}", username); // Логируем извлеченное имя пользователя

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    logger.debug("Загружены детали пользователя для: {}", username); // Логируем загрузку деталей пользователя
                    if (jwtUtils.validateToken(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        logger.info("Пользователь {} аутентифицирован с помощью JWT", username); // Логируем успешную аутентификацию
                    } else {
                        logger.warn("Не удалось проверить JWT для пользователя: {}", username); // Логируем невалидный токен
                    }
                }
            } catch (ExpiredJwtException e) {
                logger.warn("JWT истек: {}", e.getMessage());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT has expired");
                return;
            } catch (UnsupportedJwtException e) {
                logger.warn("Неподдерживаемый JWT: {}", e.getMessage());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unsupported JWT");
                return;
            } catch (MalformedJwtException e) {
                logger.warn("Неверный формат JWT: {}", e.getMessage());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT format");
                return;
            } catch (SignatureException e) {
                logger.warn("Неверная подпись JWT: {}", e.getMessage());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT signature");
                return;
            } catch (JwtAuthenticationException e) {
                logger.warn("Ошибка аутентификации JWT: {}", e.getMessage());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
                return;
            } catch (Exception e) {
                logger.error("Ошибка при обработке JWT", e);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication failed");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return request.getServletPath().equals("/api/auth/login"); // Не фильтруем /api/auth/login
    }
}
