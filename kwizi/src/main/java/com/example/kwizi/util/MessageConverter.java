package com.example.kwizi.util;


import com.example.kwizi.DTO.internal.MessageDto;
import com.example.kwizi.DTO.internal.MessageEventDto;
import com.example.kwizi.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MessageConverter {

    private final ObjectMapper objectMapper;
    private final Logger logger = LoggerFactory.getLogger(MessageConverter.class);

    @Autowired
    public MessageConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public MessageEventDto convertToEvent(String json) {
        try {
            MessageEventDto event = objectMapper.readValue(json, MessageEventDto.class);
            logger.debug("Успешно преобразован JSON в MessageEventDto: {}", event.getLogInfo());
            return event;
        } catch (Exception e) {
            logger.error("Ошибка преобразования JSON в MessageEventDto: {}", json, e);
            throw new RuntimeException("Неверный формат сообщения", e);
        }
    }

    public String convertToJson(MessageEventDto event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            logger.debug("Успешно преобразован MessageEventDto в JSON");
            return json;
        } catch (Exception e) {
            logger.error("Ошибка преобразования MessageEventDto в JSON", e);
            throw new RuntimeException("Ошибка сериализации сообщения", e);
        }
    }

    public MessageDto convertToDto(Message message) {
        MessageDto dto = new MessageDto();
        dto.setId(message.getId());
        dto.setText(message.getText());
        dto.setCreatedAt(message.getCreatedAt().toLocalDateTime());
        dto.setSenderId(message.getSender().getId());

        if (message.getChat() != null) {
            dto.setChatId(message.getChat().getId());
        }

        logger.debug("Преобразовано сообщение {} в MessageDto", message.getId());
        return dto;
    }

    public MessageEventDto createMessageEvent(String rawJson, Long senderId) {
        try {
            MessageEventDto event = objectMapper.readValue(rawJson, MessageEventDto.class);
            event.setSenderId(senderId);
            event.setTimestamp(java.time.Instant.now());
            event.validate();
            return event;
        } catch (Exception e) {
            logger.error("Ошибка создания MessageEventDto из JSON", e);
            throw new RuntimeException("Неверный формат сообщения", e);
        }
    }

    public String createSuccessResponse() {
        try {
            return objectMapper.writeValueAsString(
                    java.util.Map.of(
                            "status", "SENT",
                            "timestamp", LocalDateTime.now().toString()
                    )
            );
        } catch (Exception e) {
            logger.error("Ошибка создания ответа клиенту", e);
            return "{\"status\":\"ERROR\",\"message\":\"Ошибка формирования ответа\"}";
        }
    }

    public String createErrorResponse(String code, String message) {
        try {
            return objectMapper.writeValueAsString(
                    java.util.Map.of(
                            "type", "ERROR",
                            "code", code,
                            "message", message,
                            "timestamp", LocalDateTime.now().toString()
                    )
            );
        } catch (Exception e) {
            logger.error("Ошибка создания ответа об ошибке", e);
            return "{\"type\":\"ERROR\",\"code\":\"SERVER_ERROR\",\"message\":\"Внутренняя ошибка сервера\"}";
        }
    }


}