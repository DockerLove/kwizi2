package com.example.kwizi.scheduler;

import com.example.kwizi.repository.RevokedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
public class TokenCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(TokenCleanupScheduler.class);

    private final RevokedTokenRepository revokedTokenRepo;

    @Autowired
    public TokenCleanupScheduler(RevokedTokenRepository revokedTokenRepo) {
        this.revokedTokenRepo = revokedTokenRepo;
    }

    @Scheduled(cron = "0 0 3 * * ?") // Каждый день в 3:00
    @Transactional
    public void cleanExpiredTokens() {
        logger.info("Запуск задачи очистки просроченных токенов..."); // Начало выполнения задачи
        try {
            revokedTokenRepo.deleteExpiredTokens();
            logger.info("Удалено {} просроченных токенов."); // Сообщение об успешном завершении и количестве удаленных токенов
        } catch (Exception e) {
            logger.error("Ошибка при очистке просроченных токенов: {}", e.getMessage(), e); // Логирование ошибки с stack trace
        }
    }
}