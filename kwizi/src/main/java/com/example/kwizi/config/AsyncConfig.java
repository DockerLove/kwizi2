package com.example.kwizi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(5);
        logger.debug("Setting core pool size to: {}", executor.getCorePoolSize());

        executor.setMaxPoolSize(10);
        logger.debug("Setting max pool size to: {}", executor.getMaxPoolSize());

        executor.setQueueCapacity(500);
        logger.debug("Setting queue capacity to: {}", executor.getQueueCapacity());

        executor.setThreadNamePrefix("EmailSender-");
        logger.debug("Setting thread name prefix to: {}", executor.getThreadNamePrefix());

        executor.initialize();
        logger.info("Async task executor initialized with corePoolSize={}, maxPoolSize={}, queueCapacity={}, threadNamePrefix={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity(), executor.getThreadNamePrefix());

        return executor;
    }
}