package com.example.kwizi.controller;

import com.example.kwizi.DTO.request.EditMessageRequest;
import com.example.kwizi.DTO.response.ApiResponse;
import com.example.kwizi.DTO.response.ChatHistoryResponse;
import com.example.kwizi.service.ChatMessageService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final ChatMessageService chatMessageService;
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    public MessageController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @GetMapping("/history/{chatId}")
    public ResponseEntity<ApiResponse<Page<ChatHistoryResponse>>> getChatHistory(
            @PathVariable Long chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            Authentication authentication
    ) {
        logger.info("Запрос истории чата. ID чата: {}, страница: {}, размер: {}, пользователь: {}",
                chatId, page, size, authentication.getName());

        Page<ChatHistoryResponse> messages = chatMessageService.getChatHistory(chatId, page, size, sort, authentication.getName());

        logger.info("История чата успешно получена. ID чата: {}, сообщений на странице: {}",
                chatId, messages.getNumberOfElements());

        return ResponseEntity.ok(ApiResponse.success("История чата успешно загружена", messages));
    }

    @PatchMapping("/edit/{messageId}")
    public ResponseEntity<?> editMessage(
            @PathVariable Long messageId,
            @RequestBody @Valid EditMessageRequest request,
            Authentication authentication
    ) {
        String username = authentication.getName();

        logger.info("Запрос на редактирование сообщения. Message ID: {}, User: {}",
                messageId, username);

        chatMessageService.editMessage(messageId, request.getText(), username);

        logger.info("Сообщение успешно отредактировано. Message ID: {}, User: {}",
                messageId, username);

        return ResponseEntity.ok(ApiResponse.success("Сообщение изменено",null));
    }

    @DeleteMapping("/delete/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @PathVariable Long messageId,
            Authentication authentication) {

        String username = authentication.getName();

        logger.info("Запрос на удаление сообщения. Message ID: {}, User: {}",
                messageId, username);

        chatMessageService.deleteMessage(messageId,username);

        logger.info("Сообщение успешно удалено. Message ID: {}, User: {}",
                messageId, username);

        return ResponseEntity.ok(ApiResponse.success(
                "Сообщение успешно удалено",
                null
        ));
    }
}



