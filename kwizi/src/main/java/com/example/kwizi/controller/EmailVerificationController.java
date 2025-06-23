package com.example.kwizi.controller;

import com.example.kwizi.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body("Email успешно подтвержден!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.TEXT_HTML)
                    .body("Ошибка: " + e.getMessage());
        }
    }
}