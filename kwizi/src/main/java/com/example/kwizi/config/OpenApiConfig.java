package com.example.kwizi.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Kwizi API")
                        .version("1.0.0")
                        .description("""
                ## REST API + WebSocket
                
                ### REST API
                Стандартные HTTP endpoints для управления пользователями, чатами, сообщениями.
                
                ### WebSocket
                **Endpoint:** `ws://localhost:8250/ws?token=<ВАШ_JWT_TOKEN>`
                
                #### 🔐 Аутентификация
                1. Получите JWT токен через `POST /api/auth/login`
                
                2. Добавьте токен в query параметр: `?token=ВАШ_JWT_TOKEN`
                
                #### 📤 Отправка сообщения (клиент → сервер):
                ```json
                // Приватное сообщение пользователю
                {
                  "type": "PRIVATE",
                  "recipientId": 3,
                  "text": "Привет! Как дела?"
                }
                
                // Сообщение в групповой чат
                {
                  "type": "GROUP", 
                  "chatId": 53,
                  "text": "Всем привет!"
                }
                ```
                
                #### 📥 Получение сообщения (сервер → клиент):
                **Для групповых сообщений:**
                ```json
                {
                    "chatId": 53,
                    "type": "GROUP_MESSAGE",
                    "message": {
                        "id": 923874,
                        "chatId": 53,
                        "senderId": 1,
                        "text": "Всем привет!",
                        "createdAt": "2026-01-03T16:22:18.836313"
                    }
                }
                ```
                **Для приватных сообщений:**
                ```json
                {
                    "type": "PRIVATE_MESSAGE",
                    "message": {
                        "id": 923875,
                        "chatId": 66,
                        "senderId": 1,
                        "text": "Привет! Как дела?",
                        "createdAt": "2026-01-03T16:25:56.47818"
                    }
                }
                ```
       
                """))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8250")
                                .description("REST API Server")
                ));
    }
}