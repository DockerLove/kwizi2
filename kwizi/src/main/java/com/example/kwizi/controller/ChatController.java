package com.example.kwizi.controller;

import com.example.kwizi.DTO.request.AddChatMemberRequestDto;
import com.example.kwizi.DTO.request.CreateGroupChatRequest;
import com.example.kwizi.DTO.request.CreatePrivateChatRequest;
import com.example.kwizi.DTO.request.UpdateGroupNameRequest;
import com.example.kwizi.DTO.response.ApiResponse;
import com.example.kwizi.DTO.response.ChatPreviewResponse;
import com.example.kwizi.security.UserDetailsImpl;
import com.example.kwizi.service.ChatService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/chats")
public class ChatController {

    private final ChatService chatService;
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
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

    @PostMapping("/{chatId}/members")
    public ResponseEntity<?> addChatMember(
            @PathVariable Long chatId,
            @Valid @RequestBody AddChatMemberRequestDto addChatMemberRequestDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        String username = userDetails.getUsername();
        logger.info("Запрос на добавление участника в чат. ID чата: {}, ID пользователя для добавления: {}", chatId, addChatMemberRequestDto.getUserId());
        addChatMemberRequestDto.setChatId(chatId);
        chatService.addChatMember(addChatMemberRequestDto, username);
        logger.info("Пользователь добавлен в чат. ID чата: {}, ID добавленного пользователя: {}", chatId, addChatMemberRequestDto.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Пользователь успешно добавлен в чат", null));
    }

    @PatchMapping("/{chatId}/members/{userId}/admin")
    public ResponseEntity<?> setAdmin(
            @PathVariable Long chatId,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Long currentUserId = currentUser.getId();
        logger.info("Запрос на назначение пользователя админом. ID чата: {}, ID пользователя: {}, инициатор: {}", chatId, userId, currentUserId);
        chatService.setAdmin(chatId, userId, currentUserId);
        logger.info("Пользователь успешно назначен админом. ID чата: {}, ID пользователя: {}, инициатор: {}", chatId, userId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Пользователь успешно назначен администратором", null));
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

    @DeleteMapping("/{chatId}/members/me")
    public ResponseEntity<?> leaveChat(
            @PathVariable Long chatId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Long currentUserId = currentUser.getId();
        logger.info("Запрос на выход из чата. ID чата: {}, ID пользователя: {}", chatId, currentUserId);
        chatService.leaveChat(chatId, currentUserId);
        logger.info("Пользователь вышел из чата. ID чата: {}, ID пользователя: {}", chatId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Вы вышли из чата", null));
    }

    @PatchMapping("/{chatId}/members/{userId}/demote")
    public ResponseEntity<?> demoteAdminToMember(
            @PathVariable Long chatId,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        Long currentUserId = currentUser.getId();
        logger.info("Запрос на разжалование администратора. ID чата: {}, ID пользователя: {}, инициатор: {}", chatId, userId, currentUserId);
        chatService.demoteAdminToMember(chatId, userId, currentUserId);
        logger.info("Администратор разжалован. ID чата: {}, ID пользователя: {}, инициатор: {}", chatId, userId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Администратор разжалован до обычного участника", null));
    }

    @PatchMapping("/{chatId}/group-name")
    public ResponseEntity<ApiResponse<String>> updateGroupName(
            @PathVariable Long chatId,
            @Valid @RequestBody UpdateGroupNameRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails.getId();
        chatService.updateGroupName(chatId, request.getGroupName(), userId);
        return ResponseEntity.ok(ApiResponse.success("Название группы успешно изменено", request.getGroupName()));
    }

    @PatchMapping("/{chatId}/avatar")
    public ResponseEntity<?> updateAvatar(
            @PathVariable Long chatId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        logger.info("Запрос на изменение фото чата ID: {}", chatId);
        chatService.updateChatAvatar(chatId, file, userDetails.getId());
        logger.info("Запрос на изменение фото чата ID успешно изменено {}", chatId);
        return ResponseEntity.ok(ApiResponse.success("Фото группы успешно изменено", null));
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<Page<ChatPreviewResponse>>> getUserChats(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "0") int page) {
        Page<ChatPreviewResponse> chats = chatService.getUserChatsPreview(userDetails.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success("Самые новые чаты успешно загружены", chats));
    }
}