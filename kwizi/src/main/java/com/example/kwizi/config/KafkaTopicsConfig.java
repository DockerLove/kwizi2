package com.example.kwizi.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.util.Map;

@Configuration
public class KafkaTopicsConfig {


    @Bean
    public NewTopic privateMessagesTopic() {
        return TopicBuilder.name("private-messages")
                .partitions(7)  // Увеличено с 3 до 5
                .replicas(1)
                .configs(Map.of(
                        TopicConfig.RETENTION_MS_CONFIG, "604800000", // 7 дней
                        TopicConfig.MAX_MESSAGE_BYTES_CONFIG, "1048576" // 1MB
                ))
                .build();
    }

    @Bean
    public NewTopic privateMessagesDlqTopic() {
        return TopicBuilder.name("private-messages-dlq")
                .partitions(7)
                .replicas(1)
                .configs(Map.of(
                        TopicConfig.RETENTION_MS_CONFIG, "2592000000", // 30 дней для DLQ
                        TopicConfig.MESSAGE_TIMESTAMP_TYPE_CONFIG, "LogAppendTime"
                ))
                .build();
    }

    @Bean
    public NewTopic groupMessagesTopic() {
        return TopicBuilder.name("group-messages")
                .partitions(7)  // Увеличено с 5 до 7
                .replicas(1)
                .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "zstd") // Сжатие
                .build();
    }

    @Bean
    public NewTopic groupMessagesDlqTopic() {
        return TopicBuilder.name("group-messages-dlq")
                .partitions(7)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, "2592000000") // 30 дней
                .build();
    }
}