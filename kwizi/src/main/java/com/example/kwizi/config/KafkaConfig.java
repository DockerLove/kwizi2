package com.example.kwizi.config;

import com.example.kwizi.exception.UserOfflineException;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // Producer
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.LINGER_MS_CONFIG, 5);          // Уменьшено время батчинга
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768);      // Увеличено в 2 раза
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "zstd"); // Сжатие
        return new DefaultKafkaProducerFactory<>(config);
    }

    // Consumer
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "websocket-group");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 200);  // Увеличено с 500
        config.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 52428800); // 50MB
        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                new StringDeserializer()
        );
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            KafkaTemplate<String, String> template) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // Настройка DLQ
        DeadLetterPublishingRecoverer dlqRecoverer = new DeadLetterPublishingRecoverer(
                template,
                (record, ex) -> new TopicPartition("private-messages-dlq", record.partition())
        );

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                dlqRecoverer,
                new FixedBackOff(1000, 3) // 3 попытки с интервалом 1 сек
        );

        // Какие исключения НЕ ретраить (сразу в DLQ)
        errorHandler.addNotRetryableExceptions(UserOfflineException.class,JsonProcessingException.class);

        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }


    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, String> template) {
        // Куда отправлять "мертвые" письма
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                template,
                (record, ex) -> new TopicPartition(record.topic() + "-dlq", record.partition())
        );

        // 2 попытки с интервалом 1 секунда
        FixedBackOff backOff = new FixedBackOff(1000L, 2L);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);

        // Какие исключения не ретраить
        handler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                MessageConversionException.class
        );

        return handler;
    }

    @Bean
    public ConsumerFactory<String, String> dlqConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "dlq-retry-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props);
    }
}