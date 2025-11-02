package com.example.kwizi.scheduler;

import com.example.kwizi.websocket.UniversalChatHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DlqRetryScheduler {
    private static final Logger logger = LoggerFactory.getLogger(DlqRetryScheduler.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final UniversalChatHandler chatHandler;
    private final ObjectMapper objectMapper;

    // Храним время последней успешной доставки для каждого пользователя
    private final Map<Long, Long> lastDeliveryTimeMap = new ConcurrentHashMap<>();
    private static final long DELIVERY_COOLDOWN_MS = 300000; // 5 минут

    @Autowired
    public DlqRetryScheduler(KafkaTemplate<String, String> kafkaTemplate,
                             UniversalChatHandler chatHandler,
                             ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.chatHandler = chatHandler;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedRate = 30000)
    public void retryDlqMessages() {
        logger.info("PRIVATE DLQ отправляет сообщение");

        try (Consumer<String, String> consumer = createDlqConsumer()) {
            consumer.subscribe(Collections.singletonList("private-messages-dlq"));

            // Получаем все доступные сообщения
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
            logger.debug("Found {} messages in DLQ", records.count());

            // Группируем сообщения по получателям
            Map<Long, List<String>> messagesByUser = groupMessagesByRecipient(records);

            // Обрабатываем каждую группу
            messagesByUser.forEach((userId, messages) -> {
                if (shouldProcessUser(userId)) {
                    processUserMessages(userId, messages, consumer);
                }
            });
        } catch (Exception e) {
            logger.error("DLQ processing failed", e);
        }
        logger.info("PRIVATE DLQ отправил сообщение");
    }

    private Map<Long, List<String>> groupMessagesByRecipient(ConsumerRecords<String, String> records) {
        Map<Long, List<String>> groupedMessages = new HashMap<>();

        records.forEach(record -> {
            try {
                JsonNode message = objectMapper.readTree(record.value());
                Long recipientId = message.get("recipientId").asLong();
                groupedMessages.computeIfAbsent(recipientId, k -> new ArrayList<>()).add(record.value());
            } catch (Exception e) {
                logger.error("Failed to parse DLQ message", e);
            }
        });

        return groupedMessages;
    }

    private boolean shouldProcessUser(Long userId) {
        // Проверяем, что пользователь онлайн и не было недавней доставки
        boolean isOnline = chatHandler.isUserOnline(userId);
        boolean isCooldown = lastDeliveryTimeMap.getOrDefault(userId, 0L) >
                System.currentTimeMillis() - DELIVERY_COOLDOWN_MS;

        if (!isOnline) {
            logger.debug("User {} is offline - skipping", userId);
            return false;
        }
        if (isCooldown) {
            logger.debug("User {} is in cooldown - skipping", userId);
            return false;
        }
        return true;
    }

    private void processUserMessages(Long userId, List<String> messages, Consumer<String, String> consumer) {
        logger.info("Processing {} messages for user {}", messages.size(), userId);

        try {
            // Отправляем все сообщения пачкой
            messages.forEach(message -> {
                kafkaTemplate.send("private-messages", message);
                logger.debug("Resent message to user {}", userId);
            });

            // Обновляем время последней доставки
            lastDeliveryTimeMap.put(userId, System.currentTimeMillis());

            // Коммитим оффсеты
            consumer.commitSync();
            logger.info("Successfully processed {} messages for user {}", messages.size(), userId);
        } catch (Exception e) {
            logger.error("Failed to process messages for user {}", userId, e);
        }
    }

    private Consumer<String, String> createDlqConsumer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "dlq-retry-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "500"); // Увеличили лимит

        return new KafkaConsumer<>(props);
    }
}
