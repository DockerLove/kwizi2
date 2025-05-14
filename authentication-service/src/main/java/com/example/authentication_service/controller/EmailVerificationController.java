package com.example.authentication_service.controller;

import com.example.authentication_service.exception.JwtAuthenticationException;
import com.example.authentication_service.model.User;
import com.example.authentication_service.repository.AuthenticationRepository;
import com.example.authentication_service.security.JwtEmailVerify;
import com.example.authentication_service.security.JwtUtils;
import com.example.authentication_service.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/email-verification")
public class EmailVerificationController {

    private AuthenticationService authenticationService;

    @Autowired
    public EmailVerificationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        try {
            authenticationService.verifyEmail(token);
            return ResponseEntity.ok("Email успешно подтвержден!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Произошла ошибка: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Произошла ошибка: " + e.getMessage());
        }
    }
}