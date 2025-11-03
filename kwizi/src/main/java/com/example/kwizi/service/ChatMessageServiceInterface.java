package com.example.kwizi.service;

import com.example.kwizi.DTO.internal.MessageDto;
import com.example.kwizi.model.Message;

public interface ChatMessageServiceInterface {
    Message sendMessage(MessageDto messageDto, Long senderId);

}