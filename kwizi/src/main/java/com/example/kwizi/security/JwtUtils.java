package com.example.kwizi.security;

import com.example.kwizi.exception.JwtAuthenticationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 hours
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        /*Этот метод генерирует JWT на основе информации о пользователе (UserDetails).
        Он устанавливает claims (заявления) в JWT (например, имя пользователя),
        время выдачи и срок действия. Затем он подписывает JWT с помощью секретного ключа
        и возвращает его в виде строки.*/
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