package com.example.kwizi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

// В конфигурационном классе

@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class); // Добавили логгер

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(5);
        logger.debug("Setting core pool size to: {}", executor.getCorePoolSize()); // Логируем настройку

        executor.setMaxPoolSize(10);
        logger.debug("Setting max pool size to: {}", executor.getMaxPoolSize()); // Логируем настройку

        executor.setQueueCapacity(500);
        logger.debug("Setting queue capacity to: {}", executor.getQueueCapacity()); // Логируем настройку

        executor.setThreadNamePrefix("EmailSender-");
        logger.debug("Setting thread name prefix to: {}", executor.getThreadNamePrefix()); // Логируем настройку

        executor.initialize();
        logger.info("Async task executor initialized with corePoolSize={}, maxPoolSize={}, queueCapacity={}, threadNamePrefix={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity(), executor.getThreadNamePrefix()); // Логируем полную инициализацию

        return executor;
    }
}