package com.example.kwizi.scheduler;
import com.example.kwizi.repository.RevokedTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TokenCleanupSchedulerTest {

    @Mock
    private RevokedTokenRepository revokedTokenRepo;

    @InjectMocks
    private TokenCleanupScheduler tokenCleanupScheduler;

    @Test
    public void cleanExpiredTokens_ShouldCallDeleteExpiredTokens() {
        // Arrange
        doNothing().when(revokedTokenRepo).deleteExpiredTokens();

        // Act
        tokenCleanupScheduler.cleanExpiredTokens();

        // Assert
        verify(revokedTokenRepo).deleteExpiredTokens();
    }

    @Test
    public void cleanExpiredTokens_ShouldHandleException() {
        // Arrange
        RuntimeException exception = new RuntimeException("Simulated exception");
        doThrow(exception).when(revokedTokenRepo).deleteExpiredTokens();

        // Act
        tokenCleanupScheduler.cleanExpiredTokens();

        // Assert
        verify(revokedTokenRepo).deleteExpiredTokens(); // Ensure that the method was called even if an exception occurred

        // Можно добавить дополнительную проверку, что логгер был вызван с ошибкой, но это требует больше setup для мок логгера
    }
}