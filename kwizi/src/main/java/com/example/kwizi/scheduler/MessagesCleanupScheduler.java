package com.example.kwizi.scheduler;

import com.example.kwizi.repository.MessageRepository;
import com.example.kwizi.util.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class MessagesCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(MessagesCleanupScheduler.class);

    private final MessageRepository messageRepository;

    @Autowired
    public MessagesCleanupScheduler(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Scheduled(cron = "0 0 5 * * *", zone = "Europe/Moscow")
    @Transactional
    public void cleanupDeletedMessages() {
        logger.info("Запуск задачи очистки удаленных сообщений...");

        try {
            long thirtyDaysInMillis = TimeUnit.DAYS.toMillis(30);
            Instant cutoffDate = Instant.now().minusMillis(thirtyDaysInMillis);

            messageRepository.deleteOldDeletedMessages(cutoffDate);

            logger.info("Удалены сообщения, удаленные до {}", cutoffDate);

        } catch (Exception e) {
            logger.error("Ошибка при очистке удаленных сообщений: {}", e.getMessage(), e);
        }
    }
}