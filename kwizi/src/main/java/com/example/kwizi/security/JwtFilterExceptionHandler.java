package com.example.kwizi.security;

import com.example.kwizi.exception.JwtAuthenticationException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtFilterExceptionHandler {

    public void handleJwtException(Exception e, HttpServletResponse response) throws IOException {
        if (e instanceof ExpiredJwtException) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT истек");
        } else if (e instanceof UnsupportedJwtException) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Неподдерживаемый JWT");
        } else if (e instanceof MalformedJwtException) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Неверный формат JWT");
        } else if (e instanceof SignatureException) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Неверная подпись JWT");
        } else if (e instanceof JwtAuthenticationException) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Ошибка аутентификации");
        }
    }
}