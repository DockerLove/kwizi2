package com.example.kwizi.config;

import com.example.kwizi.repository.ChatMemberRepository;
import com.example.kwizi.service.ChatMessageService;
import com.example.kwizi.service.UserService;
import com.example.kwizi.websocket.UniversalChatHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;
@Configuration
@EnableWebSocket // Важно: НЕ @EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketConfigurer {

    private UniversalChatHandler webSocketHandler;

    @Autowired
    public WebSocketConfig(UniversalChatHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws")
                .setAllowedOrigins("*");
    }
}