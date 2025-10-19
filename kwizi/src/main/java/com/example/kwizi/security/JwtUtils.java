package com.example.kwizi.security;

import com.example.kwizi.exception.JwtAuthenticationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.UUID;
@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${jwt.secret}")
    private String secret;

    private Key key;
    private final JwtExceptionHandler jwtExceptionHandler;

    @Autowired
    public JwtUtils(JwtExceptionHandler jwtExceptionHandler) {
        this.jwtExceptionHandler = jwtExceptionHandler;
    }

    @PostConstruct
    public void setSecret() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            this.key = Keys.hmacShaKeyFor(keyBytes);
            logger.info("Секретный ключ JWT успешно инициализирован.");
        } catch (Exception e) {
            logger.error("Ошибка при инициализации секретного ключа JWT: {}", e.getMessage(), e);
            throw new IllegalStateException("Не удалось инициализировать секретный ключ JWT", e); // Re-throw as IllegalStateException
        }
    }

    public String generateToken(String username) {
        logger.info("Генерация JWT для пользователя: {}", username);

        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Имя пользователя не может быть пустым");
        }

        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .setId(jti)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 hours
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        logger.info("JWT успешно сгенерирован для пользователя: {}", username);
        return token;
    }

    public String extractJti(String token) {
        logger.debug("Извлечение JTI из токена.");

        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Токен не может быть пустым");
        }

        try {
            Claims claims = extractAllClaims(token);

            String jti = claims.getId();
            if (jti == null) {
                logger.warn("Токен не содержит JTI");
                throw new JwtAuthenticationException("Токен не содержит JTI");
            }

            logger.debug("JTI из токена: {}", jti);
            return jti;
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtAuthenticationException("Не удалось извлечь JTI из токена: " + e.getMessage(), e);
        }
    }

    public Date extractExpiration(String token) {
        logger.debug("Извлечение срока действия токена.");
        try {
            Claims claims = extractAllClaims(token);
            Date expiration = claims.getExpiration();
            logger.debug("Срок действия токена: {}", expiration);
            return expiration;
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("Не удалось извлечь срок действия токена: {}", e.getMessage());
            throw new JwtAuthenticationException("Не удалось извлечь срок действия токена", e);
        }
    }

    public String extractToken(HttpServletRequest request) {
        logger.debug("Извлечение токена из заголовка Authorization");

        if (request == null) {
            throw new IllegalArgumentException("HttpServletRequest не может быть null");
        }

        final String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new JwtAuthenticationException("Отсутствует или невалидный заголовок Authorization");
        }

        String token = authorizationHeader.substring(7);

        if (token.trim().isEmpty()) {
            throw new JwtAuthenticationException("Токен не может быть пустым");
        }

        logger.debug("Токен успешно извлечен из заголовка");
        return token;
    }

    public boolean validateToken(String token, UserDetails userDetails) {

        if (token == null || userDetails == null) {
            logger.debug("Токен или UserDetails не могут быть null");
            return false;
        }
        logger.debug("Валидация токена для пользователя: {}", userDetails.getUsername());
        String username = getUsernameFromToken(token);

        boolean isValid = username != null && username.equals(userDetails.getUsername()) && !isTokenExpired(token);

        logger.debug("Валидация токена для пользователя {}: {}", userDetails.getUsername(), isValid);
        return isValid;
    }

    public String getUsernameFromToken(String token) {
        return jwtExceptionHandler.handleJwtOperation(() -> {
            logger.debug("Получение имени пользователя из токена.");
            Claims claims = extractAllClaims(token);
            String username = claims.getSubject();
            logger.debug("Имя пользователя из токена: {}", username);
            return username;
        }, "получении имени пользователя из токена");
    }

    public boolean isTokenExpired(String token) {
        logger.debug("Проверка срока действия токена.");

        try {
            Claims claims = jwtExceptionHandler.handleJwtOperation(() ->
                            extractAllClaims(token),
                    "проверке срока действия токена"
            );

            Date expiration = claims.getExpiration();
            boolean expired = expiration.before(new Date());
            logger.debug("Токен истек: {}", expired);
            return expired;

        } catch (JwtAuthenticationException e) {
            throw new JwtAuthenticationException("Токен невалиден при проверке срока действия: " + e.getMessage());
        }
    }



    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}