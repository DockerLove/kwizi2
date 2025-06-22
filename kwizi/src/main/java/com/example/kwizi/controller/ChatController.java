package com.example.kwizi.controller;

import com.example.kwizi.DTO.internal.ChatDto;
import com.example.kwizi.DTO.request.AddChatMemberRequestDto;
import com.example.kwizi.DTO.request.CreateGroupChatRequest;
import com.example.kwizi.DTO.request.CreatePrivateChatRequest;
import com.example.kwizi.model.User;
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
@RequestMapping("/chats")
public class ChatController {

    private final ChatService chatService;

    @Autowired
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/group")
    public ResponseEntity<?> createGroupChat(@RequestBody CreateGroupChatRequest createChatRequestDto) {
        chatService.createGroupChat(createChatRequestDto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/private")
    public ResponseEntity<?> createPrivateChat(
            @Valid @RequestBody CreatePrivateChatRequest createPrivateChatRequest,
            Principal principal) {
        try {
            chatService.createPrivateChat(createPrivateChatRequest, principal.getName());
            return ResponseEntity.ok().build(); // Возвращаем ID созданного чата
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping("/{chatId}/members")
    public ResponseEntity<?> addChatMember(
            @PathVariable Long chatId,
            @RequestBody AddChatMemberRequestDto addChatMemberRequestDto
    ) {
        try {
            addChatMemberRequestDto.setChatId(chatId);
            chatService.addChatMember(addChatMemberRequestDto);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Not Found", "message", e.getMessage())
            );
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("error", "Conflict", "message", e.getMessage())
            );
        }
    }

    @PutMapping("/{chatId}/members/{userId}/admin")
    public ResponseEntity<?> setAdmin(@PathVariable Long chatId, @PathVariable Long userId) {
        chatService.setAdmin(chatId, userId);
        return ResponseEntity.ok().build();
    }

    // Controller
    @DeleteMapping("/{chatId}/members/{id}")
    public ResponseEntity<?> removeChatMember(
            @PathVariable Long chatId,
            @PathVariable Long id,
            @RequestParam("id") Long requestingUserId) {
        try {
            chatService.removeChatMember(chatId, id, requestingUserId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}