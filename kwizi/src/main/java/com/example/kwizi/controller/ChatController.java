package com.example.kwizi.controller;

import com.example.kwizi.DTO.request.AddChatMemberRequestDto;
import com.example.kwizi.DTO.request.CreateChatRequestDto;
import com.example.kwizi.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<?> addChatMember(@PathVariable Long chatId, @RequestBody AddChatMemberRequestDto addChatMemberRequestDto) {
        addChatMemberRequestDto.setChatId(chatId); // Устанавливаем chatId из пути
        chatService.addChatMember(addChatMemberRequestDto);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{chatId}/members/{userId}/admin")
    public ResponseEntity<?> setAdmin(@PathVariable Long chatId, @PathVariable Long userId) {
        chatService.setAdmin(chatId, userId);
        return ResponseEntity.ok().build();
    }
}