package com.example.kwizi.service.consumer;

import com.example.kwizi.DTO.internal.MessageDto;
import com.example.kwizi.exception.ChatNotFoundException;
import com.example.kwizi.exception.UserNotFoundException;
import com.example.kwizi.model.Message;
import com.example.kwizi.service.ChatMessageService;
import com.example.kwizi.websocket.UniversalChatHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class GroupMessageConsumer extends BaseMessageConsumer {
    private static final Logger logger = LoggerFactory.getLogger(GroupMessageConsumer.class);

    public GroupMessageConsumer(ObjectMapper objectMapper, ChatMessageService chatMessageService, UniversalChatHandler chatHandler) {
        super(objectMapper, chatMessageService, chatHandler);
    }

    @KafkaListener(topics = "group-messages", groupId = "websocket-group")
    public void consumeGroupMessage(String kafkaMessage) {
        Long senderId = null;
        try {
            JsonNode message = objectMapper.readTree(kafkaMessage);
            senderId = message.get("senderId").asLong(); // Бросает исключение, если поля нет


            Long chatId = message.get("chatId").asLong();
            String text = message.get("text").asText();

            // 1. Сохраняем в БД
            MessageDto messageDto = new MessageDto();
            messageDto.setChatId(chatId);
            messageDto.setText(text);
            Message savedMessage = chatMessageService.sendMessage(messageDto, senderId);

            sendToSession(
                    senderId,
                    formatResponse("DELIVERY_CONFIRMATION",
                            Map.of(
                                    "messageId", savedMessage.getId(),
                                    "status", "DELIVERED"
                            )
                    )
            );

            // 2. Отправляем всем участникам чата
            List<Long> members = chatMessageService.getChatMembers(chatId);
            for (Long memberId : members) {
                if (!memberId.equals(senderId)) {
                    sendToSession(
                            memberId,
                            formatResponse("GROUP_MESSAGE",
                                    Map.of(
                                            "chatId", chatId,
                                            "message", convertToDto(savedMessage)
                                    )
                            )
                    );
                }
            }


            logger.info("Group message processed in chat {} by user {}", chatId, senderId);
        }catch (UserNotFoundException e) {
            logger.warn("User not found: {}", e.getMessage());
            sendErrorToSender(senderId, "USER_NOT_FOUND", e.getMessage());
        }catch (ChatNotFoundException e){
            logger.warn("Chat not found: {}", e.getMessage());
            sendErrorToSender(senderId, "CHAT_NOT_FOUND", e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to process group message: {}", e.getMessage());
        }
    }
}