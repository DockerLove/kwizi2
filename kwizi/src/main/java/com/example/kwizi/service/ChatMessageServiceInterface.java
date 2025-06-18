package com.example.kwizi.service;

import com.example.kwizi.DTO.internal.MessageDto;
import com.example.kwizi.model.Message;

import java.util.List;

public interface ChatMessageServiceInterface {
    Message sendMessage(MessageDto messageDto, Long senderId);
    List<Message> getChatHistory(Long chatId);
}