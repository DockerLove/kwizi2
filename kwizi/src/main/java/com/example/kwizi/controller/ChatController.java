package com.example.kwizi.controller;

import com.example.kwizi.DTO.request.AddChatMemberRequestDto;
import com.example.kwizi.DTO.request.CreateChatRequestDto;
import com.example.kwizi.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/chats")
public class ChatController {

    private final ChatService chatService;

    @Autowired
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<?> createChat(@RequestBody CreateChatRequestDto createChatRequestDto) {
        chatService.createChat(createChatRequestDto);
        return ResponseEntity.ok().build();
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
}