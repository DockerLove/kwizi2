package com.example.kwizi.scheduler;

import com.example.kwizi.repository.RevokedTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@DisplayName("TokenCleanupScheduler тесты")
@ExtendWith(MockitoExtension.class)
public class TokenCleanupSchedulerTest {

    @Mock
    private RevokedTokenRepository revokedTokenRepo;

    @InjectMocks
    private TokenCleanupScheduler tokenCleanupScheduler;

    @Nested
    @DisplayName("Очистка просроченных токенов")
    class CleanExpiredTokensTests {

        @Test
        @DisplayName("Вызывает deleteExpiredTokens при успешной очистке")
        void cleanExpiredTokens_ShouldCallDeleteExpiredTokens() {
            doNothing().when(revokedTokenRepo).deleteExpiredTokens();
            tokenCleanupScheduler.cleanExpiredTokens();
            verify(revokedTokenRepo).deleteExpiredTokens();
        }

        @Test
        @DisplayName("Обрабатывает исключение при ошибке удаления")
        void cleanExpiredTokens_ShouldHandleException() {
            RuntimeException exception = new RuntimeException("Simulated exception");
            doThrow(exception).when(revokedTokenRepo).deleteExpiredTokens();
            tokenCleanupScheduler.cleanExpiredTokens();
            verify(revokedTokenRepo).deleteExpiredTokens();
        }
    }
}