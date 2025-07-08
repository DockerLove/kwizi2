package com.example.kwizi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtEmailVerify {

    private static final Logger logger = LoggerFactory.getLogger(JwtEmailVerify.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.verification.expiration}")
    private long verificationTokenExpiration;

    private Key key;

    @PostConstruct
    public void setSecret() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            this.key = Keys.hmacShaKeyFor(keyBytes);
            logger.info("Секретный ключ для верификации email успешно инициализирован");
        } catch (Exception e) {
            logger.error("Ошибка при инициализации секретного ключа для верификации email", e);
        }
    }

    public String generateVerificationToken(Long userId) {
        logger.info("Генерация токена верификации email для пользователя с ID: {}", userId);
        try {
            String token = Jwts.builder()
                    .setSubject(String.valueOf(userId))
                    .setIssuedAt(new Date(System.currentTimeMillis()))
                    .setExpiration(new Date(System.currentTimeMillis() + verificationTokenExpiration))
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();
            logger.info("Токен верификации email успешно сгенерирован для пользователя с ID: {}", userId);
            return token;
        } catch (Exception e) {
            logger.error("Ошибка при генерации токена верификации email для пользователя с ID: {}", userId, e);
            return null; // Или выбросить исключение
        }
    }

    public String getUserIdFromToken(String token) {
        logger.debug("Получение ID пользователя из токена верификации email");
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String userId = claims.getSubject();
            logger.debug("ID пользователя из токена верификации email: {}", userId);
            return userId;
        } catch (Exception e) {
            logger.warn("Ошибка при получении ID пользователя из токена верификации email: {}", e.getMessage());
            return null; // Или выбросить исключение
        }
    }


    public boolean isTokenExpired(String token) {
        logger.debug("Проверка срока действия токена верификации email");
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            Date expiration = claims.getExpiration();
            boolean expired = expiration.before(new Date());
            logger.debug("Токен верификации email истек: {}", expired);
            return expired;
        } catch (Exception e) {
            logger.warn("Ошибка при проверке срока действия токена верификации email: {}", e.getMessage());
            return true;
        }
    }
}