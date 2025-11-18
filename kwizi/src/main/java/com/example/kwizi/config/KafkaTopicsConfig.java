package com.example.kwizi.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.util.Map;

@Configuration
public class KafkaTopicsConfig {

    private static final Logger logger = LoggerFactory.getLogger(KafkaTopicsConfig.class);

    @Bean
    public NewTopic privateMessagesTopic() {
        logger.info("Создание топика для приватных сообщений");
        return TopicBuilder.name("private-messages")
                .partitions(3)  // ✅ Упростили до 3 партиций для пет-проекта
                .replicas(1)
                .configs(Map.of(
                        "retention.ms", "604800000", // 7 дней
                        "max.message.bytes", "1048576" // 1MB
                ))
                .build();
    }

    @Bean
    public NewTopic privateMessagesDlqTopic() {
        logger.info("Создание DLQ топика для приватных сообщений");
        return TopicBuilder.name("private-messages-dlq")
                .partitions(3)
                .replicas(1)
                .configs(Map.of(
                        "retention.ms", "2592000000", // 30 дней для DLQ
                        "cleanup.policy", "delete"
                ))
                .build();
    }

    @Bean
    public NewTopic groupMessagesTopic() {
        logger.info("Создание топика для групповых сообщений");
        return TopicBuilder.name("group-messages")
                .partitions(3)  // ✅ Упростили до 3 партиций
                .replicas(1)
                .configs(Map.of(
                        "retention.ms", "604800000", // 7 дней
                        "compression.type", "snappy" // Сжатие
                ))
                .build();
    }

    @Bean
    public NewTopic groupMessagesDlqTopic() {
        logger.info("Создание DLQ топика для групповых сообщений");
        return TopicBuilder.name("group-messages-dlq")
                .partitions(3)
                .replicas(1)
                .configs(Map.of(
                        "retention.ms", "2592000000", // 30 дней
                        "cleanup.policy", "delete"
                ))
                .build();
    }
}