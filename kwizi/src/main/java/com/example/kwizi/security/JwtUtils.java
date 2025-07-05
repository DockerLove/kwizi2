package com.example.kwizi.security;

import com.example.kwizi.exception.JwtAuthenticationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
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

    @Value("${jwt.secret}") //секретный ключ из application.properties
    private String secret;

    private Key key;


    @PostConstruct
    public void setSecret() { //  Удалите аргумент String secret
        if (secret != null && !secret.isEmpty()) {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            this.key = Keys.hmacShaKeyFor(keyBytes);
        } else {
            throw new IllegalStateException("JWT secret cannot be null or empty");
        }
    }
    public String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            throw new JwtAuthenticationException("Срок действия JWT токена истек", e);
        } catch (MalformedJwtException e) {
            throw new JwtAuthenticationException("JWT токен имеет неверный формат", e);
        } catch (SignatureException e) {
            throw new JwtAuthenticationException("Неверная подпись JWT токена", e);
        } catch (UnsupportedJwtException e) {
            throw new JwtAuthenticationException("Неподдерживаемый JWT токен", e);
        } catch (IllegalArgumentException e) {
            throw new JwtAuthenticationException("JWT токен недействителен", e);
        }

        /* Этот метод извлекает имя пользователя из JWT. Он проверяет
        подпись JWT и, если она действительна, извлекает имя пользователя из claims.*/
    }


    // Добавляем метод для извлечения JTI
    public String extractJti(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getId(); // Получаем уникальный идентификатор токена
        } catch (JwtException e) {
            throw new JwtAuthenticationException("Не удалось извлечь JTI из токена", e);
        }
    }

    // Добавляем метод для извлечения срока действия
    public Date extractExpiration(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration();
        } catch (JwtException e) {
            throw new JwtAuthenticationException("Не удалось извлечь срок действия токена", e);
        }
    }

    // Обновленный метод генерации токена с JTI
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        String jti = UUID.randomUUID().toString(); // Генерируем уникальный ID

        return Jwts.builder()
                .setClaims(claims)
                .setId(jti) // Устанавливаем JTI
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 часов
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractToken(HttpServletRequest request) {
        final String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }
    public boolean validateToken(String token, UserDetails userDetails) {
        String username = getUsernameFromToken(token);
        return username != null && username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        /*Этот метод проверяет, является ли JWT действительным для данного пользователя. Он проверяет подпись, срок действия и имя пользователя.*/
    }

    public boolean isTokenExpired(String token) {//истек ли токен?
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            Date expiration = claims.getExpiration();
            return expiration.before(new Date());

        }  catch (ExpiredJwtException e) {
            throw new JwtAuthenticationException("Срок действия JWT токена истек", e);
        } catch (MalformedJwtException e) {
            throw new JwtAuthenticationException("JWT токен имеет неверный формат", e);
        } catch (SignatureException e) {
            throw new JwtAuthenticationException("Неверная подпись JWT токена", e);
        } catch (UnsupportedJwtException e) {
            throw new JwtAuthenticationException("Неподдерживаемый JWT токен", e);
        } catch (IllegalArgumentException e) {
            throw new JwtAuthenticationException("JWT токен недействителен", e);
        }
    }
}