package com.example.kwizi.controller;

import com.example.kwizi.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email-verification")
@Tag(
        name = "Подтверждение Email",
        description = """
        ### Подтверждение электронной почты
        
        Этот модуль предоставляет endpoints для:
        - ✅ Подтверждения email адреса пользователя
        - 📧 Обработки ссылок из писем подтверждения
        
        ### Как работает:
        1. При регистрации пользователю отправляется email с ссылкой подтверждения
        2. Ссылка содержит уникальный токен подтверждения
        3. При переходе по ссылке email помечается как подтвержденный
        4. Пользователь получает HTML страницу с результатом операции
        """
)
public class EmailVerificationController {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationController.class);
    private final AuthenticationService authenticationService;

    @Autowired
    public EmailVerificationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @GetMapping("/verify-email")
    @Operation(
            summary = "Подтвердить email",
            description = """
            Подтверждает email адрес пользователя по токену из письма.
            
            ### Использование:
            Этот endpoint вызывается автоматически при переходе по ссылке из email.
            Пользователи получают ссылку вида:
            ```
            http://ваш-домен/api/email-verification/verify-email?token=<токен>
            ```
            
            ### Возвращает:
            HTML страницу с результатом операции.
            
            ### Примечание:
            - Токен действителен в течение 24 часов
            - После подтверждения токен становится недействительным
            - Email можно подтвердить только один раз
            """,
            security = {} // Не требует аутентификации
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "✅ Email успешно подтвержден",
                    content = @Content(
                            mediaType = "text/html",
                            schema = @Schema(
                                    description = "HTML страница с сообщением об успехе",
                                    example = "<html><body><h1>Email успешно подтвержден!</h1></body></html>"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "❌ Некорректный или просроченный токен",
                    content = @Content(
                            mediaType = "text/html",
                            schema = @Schema(
                                    description = "HTML страница с сообщением об ошибке",
                                    example = "<html><body><h1>Ошибка: Токен недействителен или просрочен</h1></body></html>"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "❌ Email уже был подтвержден ранее",
                    content = @Content(
                            mediaType = "text/html",
                            schema = @Schema(
                                    description = "HTML страница с сообщением об ошибке",
                                    example = "<html><body><h1>Ошибка: Email уже подтвержден</h1></body></html>"
                            )
                    )
            )
    })
    public ResponseEntity<String> verifyEmail(
            @Parameter(
                    description = "Токен подтверждения email из письма",
                    example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                    required = true
            )
            @RequestParam("token") String token) {

        logger.info("Получен запрос на подтверждение email с токеном: {}", token);

        try {
            authenticationService.verifyEmail(token);
            logger.info("Email успешно подтвержден для токена: {}", token);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body("""
                            <!DOCTYPE html>
                            <html lang="ru">
                            <head>
                                <meta charset="UTF-8">
                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <title>Email подтвержден</title>
                                <style>
                                    body {
                                        font-family: Arial, sans-serif;
                                        display: flex;
                                        justify-content: center;
                                        align-items: center;
                                        min-height: 100vh;
                                        margin: 0;
                                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                                    }
                                    .container {
                                        background: white;
                                        padding: 40px;
                                        border-radius: 10px;
                                        box-shadow: 0 10px 40px rgba(0,0,0,0.1);
                                        text-align: center;
                                        max-width: 500px;
                                    }
                                    h1 {
                                        color: #4CAF50;
                                        margin-bottom: 20px;
                                    }
                                    p {
                                        color: #666;
                                        margin-bottom: 30px;
                                        line-height: 1.6;
                                    }
                                    .success-icon {
                                        font-size: 60px;
                                        color: #4CAF50;
                                        margin-bottom: 20px;
                                    }
                                </style>
                            </head>
                            <body>
                                <div class="container">
                                    <div class="success-icon">✓</div>
                                    <h1>Email успешно подтвержден!</h1>
                                    <p>Ваш email адрес был успешно подтвержден. Теперь вы можете пользоваться всеми функциями приложения.</p>
                                    <p>Можете закрыть эту страницу и вернуться в приложение.</p>
                                </div>
                            </body>
                            </html>
                            """);

        } catch (IllegalArgumentException e) {
            logger.error("Ошибка подтверждения email для токена: {}", token, e);

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.TEXT_HTML)
                    .body("""
                            <!DOCTYPE html>
                            <html lang="ru">
                            <head>
                                <meta charset="UTF-8">
                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <title>Ошибка подтверждения</title>
                                <style>
                                    body {
                                        font-family: Arial, sans-serif;
                                        display: flex;
                                        justify-content: center;
                                        align-items: center;
                                        min-height: 100vh;
                                        margin: 0;
                                        background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
                                    }
                                    .container {
                                        background: white;
                                        padding: 40px;
                                        border-radius: 10px;
                                        box-shadow: 0 10px 40px rgba(0,0,0,0.1);
                                        text-align: center;
                                        max-width: 500px;
                                    }
                                    h1 {
                                        color: #f44336;
                                        margin-bottom: 20px;
                                    }
                                    p {
                                        color: #666;
                                        margin-bottom: 30px;
                                        line-height: 1.6;
                                    }
                                    .error-icon {
                                        font-size: 60px;
                                        color: #f44336;
                                        margin-bottom: 20px;
                                    }
                                </style>
                            </head>
                            <body>
                                <div class="container">
                                    <div class="error-icon">✗</div>
                                    <h1>Ошибка подтверждения email</h1>
                                    <p>Произошла ошибка: ERROR_PLACEHOLDER</p>
                                    <p>Возможные причины:</p>
                                    <ul style="text-align: left; color: #666;">
                                        <li>Ссылка устарела (действительна 24 часа)</li>
                                        <li>Email уже был подтвержден ранее</li>
                                        <li>Некорректная ссылка</li>
                                    </ul>
                                    <p>Попробуйте запросить новое письмо подтверждения.</p>
                                </div>
                            </body>
                            </html>
                            """.replace("ERROR_PLACEHOLDER", e.getMessage() != null ? e.getMessage() : "Неизвестная ошибка"));
        }
    }
}