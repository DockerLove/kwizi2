package com.example.kwizi.security;

import com.example.kwizi.exception.JwtAuthenticationException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.stereotype.Component;



@Component
public class JwtExceptionHandler {

    @FunctionalInterface
    public interface JwtOperation<T> {
        T execute() throws JwtException;
    }

    public <T> T handleJwtOperation(JwtOperation<T> operation, String operationName) {
        try {
            return operation.execute();
        } catch (ExpiredJwtException e) {
            throw new JwtAuthenticationException("Срок действия JWT токена истек при " + operationName, e);
        } catch (MalformedJwtException e) {
            throw new JwtAuthenticationException("JWT токен имеет неверный формат при " + operationName, e);
        } catch (SignatureException e) {
            throw new JwtAuthenticationException("Неверная подпись JWT токена при " + operationName, e);
        } catch (UnsupportedJwtException e) {
            throw new JwtAuthenticationException("Неподдерживаемый JWT токен при " + operationName, e);
        } catch (IllegalArgumentException e) {
            throw new JwtAuthenticationException("JWT токен недействителен при " + operationName, e);
        } catch (JwtException e) {
            throw new JwtAuthenticationException("Ошибка обработки JWT токена при " + operationName, e);
        }
    }
}