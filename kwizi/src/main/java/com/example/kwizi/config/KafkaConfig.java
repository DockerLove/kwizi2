package com.example.kwizi.config;

import com.example.kwizi.exception.ChatNotFoundException;
import com.example.kwizi.exception.MessageValidationException;
import com.example.kwizi.exception.UserNotFoundException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConfig.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // ==================== PRODUCER CONFIG ====================

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        logger.info("Инициализация ProducerFactory для брокеров: {}", bootstrapServers);

        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        configProps.put(ProducerConfig.ACKS_CONFIG, "1");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        // Настройки производительности
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

        logger.debug("Producer конфигурация: acks=1, retries=3 - оптимизировано для пет-проекта");

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory());
        logger.info("KafkaTemplate успешно создан");
        return template;
    }

    // ==================== CONSUMER CONFIG ====================

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        logger.info("Инициализация ConsumerFactory для группы: message-consumer-group");

        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "message-consumer-group");
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Ручной коммит
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100); // Оптимальный размер пачки
        configProps.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 52428800); // 50MB

        // Настройки для надежности
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000); // 30 секунд
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000); // 10 секунд

        logger.debug("Consumer конфигурация: autoCommit=false, maxPollRecords=100");

        return new DefaultKafkaConsumerFactory<>(
                configProps,
                new StringDeserializer(),
                new StringDeserializer()
        );
    }

    // ==================== LISTENER CONTAINER FACTORY ====================

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            KafkaTemplate<String, String> kafkaTemplate) {

        logger.info("Создание KafkaListenerContainerFactory");

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        factory.setConcurrency(3);

        // Настройка обработки ошибок
        DefaultErrorHandler errorHandler = createErrorHandler(kafkaTemplate);
        factory.setCommonErrorHandler(errorHandler);

        logger.debug("KafkaListenerContainerFactory настроен с concurrency=3 и обработкой ошибок");

        return factory;
    }

    // ==================== ERROR HANDLER ====================

    private DefaultErrorHandler createErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        logger.info("Настройка обработчика ошибок с DLQ");

        // Dead Letter Queue recoverer
        DeadLetterPublishingRecoverer dlqRecoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> {
                    String originalTopic = record.topic();
                    String dlqTopic = originalTopic + "-dlq";
                    logger.warn("Перенаправление сообщения в DLQ: {} -> {}", originalTopic, dlqTopic);
                    // ✅ ИСПРАВЛЕНО: используем правильный класс
                    return new org.apache.kafka.common.TopicPartition(dlqTopic, record.partition());
                }
        );

        // 3 попытки с интервалом 1 секунда, затем в DLQ
        FixedBackOff backOff = new FixedBackOff(1000L, 3L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(dlqRecoverer, backOff);

        // Исключения, которые не нужно ретраить (сразу в DLQ)
        errorHandler.addNotRetryableExceptions(
                MessageValidationException.class,
                UserNotFoundException.class,
                ChatNotFoundException.class,
                IllegalArgumentException.class
        );

        logger.debug("ErrorHandler настроен: 3 ретрая с интервалом 1сек, исключения сразу в DLQ");

        return errorHandler;
    }
}