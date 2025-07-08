package com.example.kwizi.controller;

import com.example.kwizi.service.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email-verification")
public class EmailVerificationController {
    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationController.class);


    private AuthenticationService authenticationService;

    @Autowired
    public EmailVerificationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        logger.info("Получен запрос на подтверждение email с токеном: {}", token); // Логируем получение запроса

        try {
            authenticationService.verifyEmail(token);
            logger.info("Email успешно подтвержден для токена: {}", token); // Логируем успешное подтверждение
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body("Email успешно подтвержден! Можете закрыть эту страницу.");
        } catch (IllegalArgumentException e) {
            logger.warn("Ошибка при подтверждении email для токена {}: {}", token, e.getMessage()); // Логируем ошибку
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.TEXT_HTML)
                    .body("Ошибка: " + e.getMessage());
        }
    }
}