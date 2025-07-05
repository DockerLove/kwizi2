package com.example.kwizi.scheduler;

import com.example.kwizi.repository.RevokedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TokenCleanupScheduler {
    private final RevokedTokenRepository revokedTokenRepo;
    @Autowired
    public TokenCleanupScheduler(RevokedTokenRepository revokedTokenRepo) {
        this.revokedTokenRepo = revokedTokenRepo;
    }

    @Scheduled(cron = "0 0 3 * * ?") // Каждый день в 3:00
    @Transactional
    public void cleanExpiredTokens() {
        revokedTokenRepo.deleteExpiredTokens();
    }
}