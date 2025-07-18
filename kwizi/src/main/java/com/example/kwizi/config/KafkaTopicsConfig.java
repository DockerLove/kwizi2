package com.example.kwizi.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    @Bean
    public NewTopic privateMessagesTopic() {
        return TopicBuilder.name("private-messages")
                .partitions(3)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, "604800000") // 7 дней
                .build();
    }

    @Bean
    public NewTopic groupMessagesTopic() {
        return TopicBuilder.name("group-messages")
                .partitions(5)
                .replicas(1)
                .build();
    }
}