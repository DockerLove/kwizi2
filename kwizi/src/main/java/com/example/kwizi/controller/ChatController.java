package com.example.kwizi.controller;

import com.example.kwizi.DTO.request.AddChatMemberRequestDto;
import com.example.kwizi.DTO.request.CreateGroupChatRequest;
import com.example.kwizi.DTO.request.CreatePrivateChatRequest;
import com.example.kwizi.DTO.request.EditMessageRequest;
import com.example.kwizi.DTO.response.ApiResponse;
import com.example.kwizi.DTO.response.ChatHistoryResponse;
import com.example.kwizi.security.UserDetailsImpl;
import com.example.kwizi.service.ChatMessageService;
import com.example.kwizi.service.ChatService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chats")
public class ChatController {

    private final ChatService chatService;
    private final ChatMessageService chatMessageService;
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);


    @Autowired
    public ChatController(ChatService chatService,ChatMessageService chatMessageService) {
        this.chatService = chatService;
        this.chatMessageService = chatMessageService;
    }

    @GetMapping("/{chatId}/messages")
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
    @PatchMapping("/messages/{messageId}")
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

    @PostMapping("/group")
    public ResponseEntity<?> createGroupChat(
            @Valid @RequestBody CreateGroupChatRequest createChatRequestDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        String username = userDetails.getUsername();
        logger.info("Запрос на создание группового чата. Инициатор: {}, название чата: {}", username, createChatRequestDto.getGroupName());
        chatService.createGroupChat(createChatRequestDto, username);
        logger.info("Групповой чат успешно создан. Название чата: {}, Инициатор: {}", createChatRequestDto.getGroupName(), username);
        return ResponseEntity.ok(ApiResponse.success("Групповой чат успешно создан", null));
    }

    @PostMapping("/private")
    public ResponseEntity<?> createPrivateChat(
            @Valid @RequestBody CreatePrivateChatRequest createPrivateChatRequest,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        String username = userDetails.getUsername();
        logger.info("Запрос на создание приватного чата. Инициатор: {}, получатель: {}", username, createPrivateChatRequest.getRecipientUsername());
        chatService.createPrivateChat(createPrivateChatRequest, username);
        logger.info("Приватный чат успешно создан. Инициатор: {}, получатель: {}", username, createPrivateChatRequest.getRecipientUsername());
        return ResponseEntity.ok(ApiResponse.success("Приватный чат успешно создан", null));
    }

    @PostMapping("/{chatId}/members")
    public ResponseEntity<?> addChatMember(
            @PathVariable Long chatId,
            @RequestBody AddChatMemberRequestDto addChatMemberRequestDto
    ) {
        logger.info("Запрос на добавление участника в чат. ID чата: {}, ID пользователя для добавления: {}", chatId, addChatMemberRequestDto.getUserId());
        addChatMemberRequestDto.setChatId(chatId);
        chatService.addChatMember(addChatMemberRequestDto);
        logger.info("Пользователь добавлен в чат. ID чата: {}, ID добавленного пользователя: {}", chatId, addChatMemberRequestDto.getUserId());
        return ResponseEntity.ok(
                ApiResponse.success("Пользователь успешно добавлен в чат", null));
    }

    @PutMapping("/{chatId}/members/{userId}/admin")
    public ResponseEntity<?> setAdmin(
            @PathVariable Long chatId,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Long currentUserId = currentUser.getId();
        logger.info("Запрос на назначение пользователя админом. ID чата: {}, ID пользователя: {}, инициатор: {}", chatId, userId, currentUserId);
        chatService.setAdmin(chatId, userId, currentUserId);
        logger.info("Пользователь успешно назначен админом. ID чата: {}, ID пользователя: {}, инициатор: {}", chatId, userId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Пользователь успешно назначен админом", null));
    }

    @DeleteMapping("/{chatId}/members/{id}")
    public ResponseEntity<?> removeChatMember(
            @PathVariable Long chatId,
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Long currentUserId = currentUser.getId();
        logger.info("Запрос на удаление участника из чата. ID чата: {}, ID удаляемого пользователя: {}, инициатор: {}", chatId, id, currentUserId);
        chatService.removeChatMember(chatId, id, currentUserId);
        logger.info("Пользователь удален из чата. ID чата: {}, ID удаленного пользователя: {}, инициатор: {}", chatId, id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Пользователь удален из чата", null));
    }
}