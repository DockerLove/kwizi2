package com.example.kwizi.security;

import com.example.kwizi.exception.JwtAuthenticationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${jwt.secret}")
    private String secret;

    private Key key;

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

    public String getUsernameFromToken(String token) {
        logger.debug("Получение имени пользователя из токена.");
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String username = claims.getSubject();
            logger.debug("Имя пользователя из токена: {}", username);
            return username;
        } catch (ExpiredJwtException e) {
            logger.warn("Срок действия JWT токена истек: {}", e.getMessage());
            throw new JwtAuthenticationException("Срок действия JWT токена истек", e);
        } catch (MalformedJwtException e) {
            logger.warn("JWT токен имеет неверный формат: {}", e.getMessage());
            throw new JwtAuthenticationException("JWT токен имеет неверный формат", e);
        } catch (SignatureException e) {
            logger.warn("Неверная подпись JWT токена: {}", e.getMessage());
            throw new JwtAuthenticationException("Неверная подпись JWT токена", e);
        } catch (UnsupportedJwtException e) {
            logger.warn("Неподдерживаемый JWT токен: {}", e.getMessage());
            throw new JwtAuthenticationException("Неподдерживаемый JWT токен", e);
        } catch (IllegalArgumentException e) {
            logger.warn("JWT токен недействителен: {}", e.getMessage());
            throw new JwtAuthenticationException("JWT токен недействителен", e);
        }
    }

    public String extractJti(String token) {
        logger.debug("Извлечение JTI из токена.");
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String jti = claims.getId();
            logger.debug("JTI из токена: {}", jti);
            return jti;
        } catch (Exception e) {
            logger.warn("Не удалось извлечь JTI из токена: {}", e.getMessage());
            throw new JwtAuthenticationException("Не удалось извлечь JTI из токена", e);
        }
    }

    public Date extractExpiration(String token) {
        logger.debug("Извлечение срока действия токена.");
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            Date expiration = claims.getExpiration();
            logger.debug("Срок действия токена: {}", expiration);
            return expiration;
        } catch (Exception e) {
            logger.warn("Не удалось извлечь срок действия токена: {}", e.getMessage());
            throw new JwtAuthenticationException("Не удалось извлечь срок действия токена", e);
        }
    }

    public String generateToken(String username) {
        logger.info("Генерация JWT для пользователя: {}", username);
        Map<String, Object> claims = new HashMap<>();
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .setClaims(claims)
                .setId(jti)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 hours
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        logger.info("JWT успешно сгенерирован для пользователя: {}", username);
        return token;
    }

    public String extractToken(HttpServletRequest request) {
        logger.debug("Извлечение токена из заголовка Authorization.");
        final String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            logger.debug("Извлеченный токен: {}", token);
            return token;
        }
        logger.debug("Заголовок Authorization отсутствует или не начинается с Bearer.");
        return null;
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        logger.debug("Валидация токена для пользователя: {}", userDetails.getUsername());
        String username = getUsernameFromToken(token);
        boolean isValid = username != null && username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        logger.debug("Валидация токена для пользователя {}: {}", userDetails.getUsername(), isValid);
        return isValid;
    }

    public boolean isTokenExpired(String token) {
        logger.debug("Проверка срока действия токена.");
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            Date expiration = claims.getExpiration();
            boolean expired = expiration.before(new Date());
            logger.debug("Токен истек: {}", expired);
            return expired;
        } catch (ExpiredJwtException e) {
            logger.warn("Срок действия токена истек: {}", e.getMessage());
            return true;
        } catch (MalformedJwtException e) {
            logger.warn("JWT токен имеет неверный формат: {}", e.getMessage());
            return true;
        } catch (SignatureException e) {
            logger.warn("Неверная подпись JWT токена: {}", e.getMessage());
            return true;
        } catch (UnsupportedJwtException e) {
            logger.warn("Неподдерживаемый JWT токен: {}", e.getMessage());
            return true;
        } catch (IllegalArgumentException e) {
            logger.warn("JWT токен недействителен: {}", e.getMessage());
            return true;
        }
    }
}