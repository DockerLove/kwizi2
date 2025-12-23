package com.example.kwizi.scheduler;

import com.example.kwizi.DTO.internal.MessageEventDto;
import com.example.kwizi.util.MessageConverter;
import com.example.kwizi.websocket.UniversalChatHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class DlqRetryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DlqRetryScheduler.class);

    private final MessageConverter messageConverter;
    private final UniversalChatHandler chatHandler;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private final Set<Long> messagesInProgress = ConcurrentHashMap.newKeySet();

    @Autowired
    public DlqRetryScheduler(MessageConverter messageConverter,
                             UniversalChatHandler chatHandler,
                             ObjectMapper objectMapper,
                             KafkaTemplate<String, String> kafkaTemplate) {
        this.messageConverter = messageConverter;
        this.chatHandler = chatHandler;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    // ==================== KAFKA LISTENER ДЛЯ НОВЫХ СООБЩЕНИЙ ====================

    @KafkaListener(topics = "private-messages-dlq", groupId = "dlq-retry-group")
    public void retryPrivateMessages(String dlqMessage) {
        logger.info("📨 Получено сообщение из DLQ");

        try {
            // ✅ ОБРАБАТЫВАЕМ ОБА ФОРМАТА: оригинальный MessageEventDto и наш обернутый DLQ формат
            MessageEventDto event;
            Long messageId;
            Long recipientId;

            try {
                // Пытаемся распарсить как наш DLQ формат
                JsonNode dlqNode = objectMapper.readTree(dlqMessage);

                if (dlqNode.has("originalEvent") && dlqNode.has("messageId")) {
                    // Это наш обернутый DLQ формат
                    JsonNode originalEventNode = dlqNode.get("originalEvent");
                    messageId = dlqNode.get("messageId").asLong();
                    recipientId = dlqNode.get("recipientId").asLong();
                    event = objectMapper.convertValue(originalEventNode, MessageEventDto.class);

                    logger.info("Обработка DLQ сообщения {}: {} -> {}",
                            messageId, event.getSenderId(), recipientId);
                } else {
                    // Это оригинальный MessageEventDto (пришло напрямую из Kafka error handler)
                    event = messageConverter.convertToEvent(dlqMessage);
                    messageId = null; // ID сообщения неизвестен
                    recipientId = event.getRecipientId();

                    logger.info("Обработка оригинального DLQ сообщения: {} -> {}",
                            event.getSenderId(), recipientId);
                }

            } catch (Exception e) {
                // Если не удалось распарсить как наш формат, пробуем как оригинальный MessageEventDto
                event = messageConverter.convertToEvent(dlqMessage);
                messageId = null;
                recipientId = event.getRecipientId();
                logger.info("Обработка MessageEventDto из DLQ: {} -> {}",
                        event.getSenderId(), recipientId);
            }

            // Мгновенная доставка для новых сообщений
            if (chatHandler.isUserOnline(recipientId)) {
                boolean delivered = sendPrivateMessageToUser(event, messageId);
                if (delivered) {
                    logger.info("✅ Сообщение доставлено мгновенно пользователю {}", recipientId);
                }
            } else {
                logger.debug("👤 Пользователь {} оффлайн, сообщение остается в DLQ", recipientId);
            }

        } catch (Exception e) {
            logger.error("💥 Ошибка обработки сообщения из DLQ: {}", e.getMessage());
            logger.debug("Содержимое сообщения: {}", dlqMessage);
        }
    }

    @KafkaListener(topics = "group-messages-dlq", groupId = "dlq-retry-group")
    public void retryGroupMessages(String dlqMessage) {
        logger.info("📨 Получено сообщение из group-messages-dlq");

        try {
            JsonNode dlqNode = objectMapper.readTree(dlqMessage);

            // Проверяем что это групповое сообщение
            if (!dlqNode.has("messageType") || !"GROUP".equals(dlqNode.get("messageType").asText())) {
                logger.debug("Не групповое сообщение, пропускаем");
                return;
            }

            JsonNode originalEventNode = dlqNode.get("originalEvent");
            Long recipientId = dlqNode.get("recipientId").asLong();
            Long messageId = dlqNode.has("messageId") ? dlqNode.get("messageId").asLong() : null;
            Long chatId = dlqNode.get("chatId").asLong();

            MessageEventDto event = objectMapper.convertValue(originalEventNode, MessageEventDto.class);

            logger.info("Обработка группового DLQ сообщения {}: чат {}, получатель {}",
                    messageId, chatId, recipientId);

            // Доставляем если пользователь онлайн
            if (chatHandler.isUserOnline(recipientId)) {
                boolean delivered = sendGroupMessageToUser(event, messageId, chatId, recipientId);
                if (delivered) {
                    logger.info("✅ Групповое сообщение {} доставлено пользователю {}", messageId, recipientId);
                }
            } else {
                logger.debug("👤 Участник {} оффлайн, сообщение {} остается в DLQ", recipientId, messageId);
            }

        } catch (Exception e) {
            logger.error("💥 Ошибка обработки группового сообщения из DLQ: {}", e.getMessage(), e);
        }
    }

    // ✅ НОВЫЙ МЕТОД: Отправка группового сообщения пользователю
    private boolean sendGroupMessageToUser(MessageEventDto event, Long messageId, Long chatId, Long recipientId) {
        try {
            // ✅ ФИКС: используем recipientId из параметра
            if (recipientId == null) {
                logger.error("❌ recipientId parameter is NULL");
                return false;
            }

            Long actualMessageId = messageId != null ? messageId : System.currentTimeMillis();

            Map<String, Object> messageData = new HashMap<>();
            messageData.put("id", actualMessageId);
            messageData.put("text", event.getText());
            messageData.put("senderId", event.getSenderId());
            messageData.put("timestamp", System.currentTimeMillis());

            Map<String, Object> message = new HashMap<>();
            message.put("type", "GROUP_MESSAGE");
            message.put("chatId", chatId);
            message.put("message", messageData);
            message.put("isFromDlq", true);

            String payload = objectMapper.writeValueAsString(message);

            logger.debug("Отправка сообщения пользователю {}", recipientId);
            chatHandler.sendToUser(recipientId, payload);

            return true;

        } catch (Exception e) {
            logger.error("❌ Ошибка отправки: {}", e.getMessage(), e);
            return false;
        }
    }

    // ==================== АКТИВНАЯ ПРОВЕРКА DLQ ====================

    @Scheduled(fixedDelay = 10000)
    public void scheduledDlqRetry() {
        logger.info("🔄 Запуск активной проверки DLQ...");

        // Проверяем оба DLQ топика
        checkDlqTopic("private-messages-dlq");
        checkDlqTopic("group-messages-dlq");

        logger.info("✅ Активная проверка всех DLQ завершена");
    }
    private void checkDlqTopic(String topicName) {
        logger.debug("🔍 Проверка DLQ топика: {}", topicName);

        try {
            org.apache.kafka.clients.consumer.KafkaConsumer<String, String> consumer = createDlqConsumer();

            try {
                consumer.subscribe(java.util.Collections.singletonList(topicName));

                org.apache.kafka.clients.consumer.ConsumerRecords<String, String> records =
                        consumer.poll(java.time.Duration.ofSeconds(2));

                if (records.isEmpty()) {
                    logger.debug("DLQ {} пуст", topicName);
                    return;
                }

                logger.info("Найдено {} сообщений в DLQ {}", records.count(), topicName);
                int deliveredCount = 0;
                int alreadyInProgressCount = 0;
                int skippedCount = 0;

                var offsetsToCommit = new java.util.HashMap<org.apache.kafka.common.TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata>();

                for (var record : records) {
                    try {
                        // ✅ ИЗВЛЕКАЕМ messageId безопасно
                        Long messageId = extractMessageIdSafe(record.value());
                        if (messageId == null) {
                            logger.warn("Не удалось извлечь messageId из сообщения в {}, пропускаем", topicName);
                            skippedCount++;
                            continue;
                        }

                        if (messagesInProgress.contains(messageId)) {
                            logger.debug("⏳ Сообщение {} уже в процессе доставки, пропускаем", messageId);
                            alreadyInProgressCount++;
                            continue;
                        }

                        messagesInProgress.add(messageId);

                        boolean delivered = processDlqRecordFromTopic(record, offsetsToCommit, topicName);
                        if (delivered) {
                            deliveredCount++;
                        }

                    } catch (Exception e) {
                        logger.error("Ошибка обработки записи из DLQ {}: {}", topicName, e.getMessage());
                        skippedCount++;
                    }
                }

                if (!offsetsToCommit.isEmpty()) {
                    consumer.commitSync(offsetsToCommit);
                    logger.info("✅ Удалено {} сообщений из DLQ {}", offsetsToCommit.size(), topicName);
                }

                logger.info("Проверка {} завершена. Доставлено: {}, В процессе: {}, Пропущено: {}, Всего: {}",
                        topicName, deliveredCount, alreadyInProgressCount, skippedCount, records.count());

            } finally {
                consumer.close();
            }

        } catch (Exception e) {
            logger.error("💥 Ошибка при проверке DLQ {}: {}", topicName, e.getMessage(), e);
        }
    }

    private boolean processDlqRecordFromTopic(
            org.apache.kafka.clients.consumer.ConsumerRecord<String, String> record,
            java.util.Map<org.apache.kafka.common.TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata> offsetsToCommit,
            String topicName) {

        Long messageId = null;

        try {
            // ✅ БЕЗОПАСНО ИЗВЛЕКАЕМ ДАННЫЕ ИЗ DLQ СООБЩЕНИЯ
            JsonNode dlqNode = objectMapper.readTree(record.value());
            MessageEventDto event;
            Long recipientId;

            if (dlqNode.has("originalEvent") && dlqNode.has("recipientId")) {
                // Наш обернутый формат
                JsonNode originalEventNode = dlqNode.get("originalEvent");
                messageId = dlqNode.has("messageId") ? dlqNode.get("messageId").asLong() : null;
                recipientId = dlqNode.get("recipientId").asLong();
                event = objectMapper.convertValue(originalEventNode, MessageEventDto.class);

                // Проверяем тип сообщения для групповых
                if ("group-messages-dlq".equals(topicName)) {
                    return processGroupDlqMessage(event, messageId, recipientId, dlqNode, record, offsetsToCommit);
                } else {
                    return processPrivateDlqMessage(event, messageId, recipientId, record, offsetsToCommit);
                }
            } else {
                // Оригинальный MessageEventDto
                event = messageConverter.convertToEvent(record.value());
                messageId = null;
                recipientId = event.getRecipientId();

                if ("group-messages-dlq".equals(topicName)) {
                    return processGroupDlqMessage(event, messageId, recipientId, dlqNode, record, offsetsToCommit);
                } else {
                    return processPrivateDlqMessage(event, messageId, recipientId, record, offsetsToCommit);
                }
            }

        } catch (Exception e) {
            logger.error("❌ Ошибка обработки сообщения из {}: {}", topicName, e.getMessage());
            return false;
        } finally {
            if (messageId != null) {
                messagesInProgress.remove(messageId);
            }
        }
    }

    private boolean processPrivateDlqMessage(MessageEventDto event, Long messageId, Long recipientId,
                                             org.apache.kafka.clients.consumer.ConsumerRecord<String, String> record,
                                             java.util.Map<org.apache.kafka.common.TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata> offsetsToCommit) {

        logger.debug("Обработка приватного сообщения для пользователя {} (онлайн: {})",
                recipientId, chatHandler.isUserOnline(recipientId));

        if (chatHandler.isUserOnline(recipientId)) {
            boolean delivered = sendPrivateMessageToUser(event, messageId);
            if (delivered) {
                logger.info("✅ Приватное сообщение ДОСТАВЛЕНО пользователю {}", recipientId);

                // Удаляем из DLQ
                var topicPartition = new org.apache.kafka.common.TopicPartition(record.topic(), record.partition());
                var offsetAndMetadata = new org.apache.kafka.clients.consumer.OffsetAndMetadata(record.offset() + 1);
                offsetsToCommit.put(topicPartition, offsetAndMetadata);

                return true;
            }
        }

        return false;
    }

    private boolean processGroupDlqMessage(MessageEventDto event, Long messageId, Long recipientId, JsonNode dlqNode,
                                           org.apache.kafka.clients.consumer.ConsumerRecord<String, String> record,
                                           java.util.Map<org.apache.kafka.common.TopicPartition, org.apache.kafka.clients.consumer.OffsetAndMetadata> offsetsToCommit) {

        Long chatId = dlqNode.has("chatId") ? dlqNode.get("chatId").asLong() : event.getChatId();

        logger.debug("processGroupDlqMessage: event.recipientId={}, recipientId param={}, chatId={}",
                event != null ? event.getRecipientId() : "event is null",
                recipientId, chatId);

        Long targetRecipientId = recipientId != null ? recipientId :
                (event != null ? event.getRecipientId() : null);

        if (targetRecipientId == null) {
            logger.error("❌ targetRecipientId is NULL. DLQ message: {}", record.value());
            return false;
        }

        if (chatHandler.isUserOnline(targetRecipientId)) {
            boolean delivered = sendGroupMessageToUser(event, messageId, chatId, recipientId);
            if (delivered) {
                logger.info("✅ Групповое сообщение ДОСТАВЛЕНО пользователю {} (чат: {})", targetRecipientId, chatId);

                // ✅ ФИКС: Убедимся что добавляем в offsetsToCommit
                var topicPartition = new org.apache.kafka.common.TopicPartition(record.topic(), record.partition());
                var offsetAndMetadata = new org.apache.kafka.clients.consumer.OffsetAndMetadata(record.offset() + 1);
                offsetsToCommit.put(topicPartition, offsetAndMetadata);

                logger.debug("📝 Добавлен оффсет для коммита: topic={}, partition={}, offset={}",
                        record.topic(), record.partition(), record.offset());

                return true;
            }
        }

        return false;
    }

    // ==================== ОТПРАВКА СООБЩЕНИЙ ====================

    private boolean sendPrivateMessageToUser(MessageEventDto event, Long messageId) {
        try {
            Map<String, Object> message = Map.of(
                    "type", "PRIVATE_MESSAGE",
                    "message", Map.of(
                            "id", messageId != null ? messageId : System.currentTimeMillis(),
                            "text", event.getText(),
                            "senderId", event.getSenderId(),
                            "timestamp", System.currentTimeMillis()
                    ),
                    "isFromDlq", true
            );

            String payload = objectMapper.writeValueAsString(message);
            chatHandler.sendToUser(event.getRecipientId(), payload);

            return true;

        } catch (Exception e) {
            logger.error("❌ Ошибка отправки сообщения: {}", e.getMessage());
            return false;
        }
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    private Long extractMessageIdSafe(String dlqMessage) {
        try {
            JsonNode dlqNode = objectMapper.readTree(dlqMessage);

            // ✅ 1. Новый формат: {"messageId": 123, ...}
            if (dlqNode.has("messageId") && !dlqNode.get("messageId").isNull()) {
                return dlqNode.get("messageId").asLong();
            }

            // ✅ 2. Если нет messageId - генерируем из хеша
            return (long) Math.abs(dlqMessage.hashCode());

        } catch (Exception e) {
            // ✅ 3. Если вообще не парсится - генерируем ID
            return (long) Math.abs(dlqMessage.hashCode());
        }
    }

    private org.apache.kafka.clients.consumer.KafkaConsumer<String, String> createDlqConsumer() {
        java.util.Properties props = new java.util.Properties();
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, "dlq-active-scheduler-group");
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringDeserializer.class.getName());
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringDeserializer.class.getName());
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100");

        return new org.apache.kafka.clients.consumer.KafkaConsumer<>(props);
    }

    @Scheduled(fixedRate = 30000)
    public void cleanupInProgress() {
        int initialSize = messagesInProgress.size();
        if (initialSize > 0) {
            logger.debug("🧹 Очистка зависших сообщений в процессе доставки: {}", initialSize);
            messagesInProgress.clear();
        }
    }

    @Scheduled(fixedRate = 60000)
    public void healthCheck() {
        logger.debug("❤️ DLQ Scheduler активен. Сообщений в процессе доставки: {}",
                messagesInProgress.size());
    }
}