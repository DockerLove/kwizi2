package com.example.kwizi.controller;

import com.example.kwizi.DTO.internal.ChatDto;
import com.example.kwizi.DTO.request.AddChatMemberRequestDto;
import com.example.kwizi.DTO.request.CreateGroupChatRequest;
import com.example.kwizi.DTO.request.CreatePrivateChatRequest;
import com.example.kwizi.DTO.response.ApiResponse;
import com.example.kwizi.model.User;
import com.example.kwizi.security.UserDetailsImpl;
import com.example.kwizi.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/chats")
public class ChatController {

    private final ChatService chatService;

    @Autowired
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/group")
    public ResponseEntity<?> createGroupChat(@Valid @RequestBody CreateGroupChatRequest createChatRequestDto, @AuthenticationPrincipal UserDetailsImpl userDetails) {
            chatService.createGroupChat(createChatRequestDto, userDetails.getUsername());
            return ResponseEntity.ok(ApiResponse.success("Групповой чат успешно создан",null));
    }

    @PostMapping("/private")
    public ResponseEntity<?> createPrivateChat(
            @Valid @RequestBody CreatePrivateChatRequest createPrivateChatRequest,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        chatService.createPrivateChat(createPrivateChatRequest, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Приватный чат успешно создан",null));

    }
    @PostMapping("/{chatId}/members")
    public ResponseEntity<?> addChatMember(
            @PathVariable Long chatId,
            @RequestBody AddChatMemberRequestDto addChatMemberRequestDto
    ) {
            addChatMemberRequestDto.setChatId(chatId);
            chatService.addChatMember(addChatMemberRequestDto);
            return ResponseEntity.ok(
                    ApiResponse.success("Пользователь успешно добавлен в чат", null));
    }

    @PutMapping("/{chatId}/members/{userId}/admin")
    public ResponseEntity<?> setAdmin(
            @PathVariable Long chatId,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
            chatService.setAdmin(chatId, userId, currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("Пользователь успешно назначен админом",null));

    }
    //todo посмотреть про Put и Post

    // Controller
    @DeleteMapping("/{chatId}/members/{id}")
    public ResponseEntity<?> removeChatMember(
            @PathVariable Long chatId,
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

            chatService.removeChatMember(chatId, id, currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success("Пользователь удален из чата",null));

    }
}