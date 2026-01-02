package com.example.kwizi.controller;

import com.example.kwizi.service.AuthenticationService;
import com.example.kwizi.service.RegistrationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Ключевые интеграционные тесты API")
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private RegistrationService registrationService;

    @Test
    @DisplayName("✅ 1. Успешное подтверждение email")
    void verifyEmail_Success() throws Exception {
        String token = "valid-verification-token-123";
        doNothing().when(authenticationService).verifyEmail(token);

        mockMvc.perform(get("/api/email-verification/verify-email")
                        .param("token", token)
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string("Email успешно подтвержден! Можете закрыть эту страницу."));
    }

    @Test
    @DisplayName("❌ 1. Подтверждение email с невалидным токеном")
    void verifyEmail_InvalidToken_ReturnsBadRequest() throws Exception {
        String token = "invalid-token";
        String errorMessage = "Невалидный или просроченный токен";
        doThrow(new IllegalArgumentException(errorMessage))
                .when(authenticationService).verifyEmail(token);

        mockMvc.perform(get("/api/email-verification/verify-email")
                        .param("token", token)
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string("Ошибка: " + errorMessage));
    }

    @Test
    @DisplayName("✅ 2. Успешная регистрация пользователя")
    void registerUser_Success() throws Exception {
        String requestJson = """
            {
                "username": "testuser",
                "password": "Password123!",
                "email": "test@example.com",
                "firstName": "John",
                "lastName": "Doe"
            }
            """;

        doNothing().when(registrationService).registerUser(any());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Пользователь успешно зарегистрирован"));
    }

    @Test
    @DisplayName("❌ 2. Регистрация с уже существующим username")
    void registerUser_DuplicateUsername_ReturnsError() throws Exception {
        String requestJson = """
            {
                "username": "existinguser",
                "password": "Password123!",
                "email": "new@example.com",
                "firstName": "John",
                "lastName": "Doe"
            }
            """;

        doThrow(new RuntimeException("Пользователь с таким именем уже существует"))
                .when(registrationService).registerUser(any());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isInternalServerError());
    }
}