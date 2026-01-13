package com.example.kwizi.scheduler;

import com.example.kwizi.repository.MessageRepository;
import com.example.kwizi.util.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@DisplayName("MessagesCleanupScheduler тесты")
@ExtendWith(MockitoExtension.class)
public class MessagesCleanupSchedulerTest {

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private MessagesCleanupScheduler messagesCleanupScheduler;

    @Nested
    @DisplayName("Очистка удаленных сообщений")
    class CleanupDeletedMessagesTests {

        @Test
        @DisplayName("Успешно удаляет старые удаленные сообщения")
        void cleanupDeletedMessages_ShouldCallDeleteOldDeletedMessages() {
            doNothing().when(messageRepository).deleteOldDeletedMessages(any(Instant.class));

            messagesCleanupScheduler.cleanupDeletedMessages();
            
            verify(messageRepository, times(1)).deleteOldDeletedMessages(any(Instant.class));
        }

        @Test
        @DisplayName("Передает корректную дату отсечения (30 дней назад)")
        void cleanupDeletedMessages_ShouldPassCorrectCutoffDate() {
            doNothing().when(messageRepository).deleteOldDeletedMessages(any(Instant.class));

            messagesCleanupScheduler.cleanupDeletedMessages();
            
            verify(messageRepository).deleteOldDeletedMessages(argThat(cutoffDate -> {
                Instant now = Instant.now();
                Instant thirtyDaysAgo = now.minusMillis(TimeUnit.DAYS.toMillis(30));
                long diffMillis = Math.abs(cutoffDate.toEpochMilli() - thirtyDaysAgo.toEpochMilli());
                return diffMillis < 1000;
            }));
        }

        @Test
        @DisplayName("Обрабатывает исключение при ошибке удаления")
        void cleanupDeletedMessages_ShouldHandleRepositoryException() {
            RuntimeException exception = new RuntimeException("Simulated database exception");
            doThrow(exception).when(messageRepository).deleteOldDeletedMessages(any(Instant.class));

            assertDoesNotThrow(() -> messagesCleanupScheduler.cleanupDeletedMessages());

            verify(messageRepository, times(1)).deleteOldDeletedMessages(any(Instant.class));
        }

        @Test
        @DisplayName("Не выбрасывает исключение при пустой БД")
        void cleanupDeletedMessages_ShouldHandleEmptyDatabase() {
            doNothing().when(messageRepository).deleteOldDeletedMessages(any(Instant.class));
            
            assertDoesNotThrow(() -> messagesCleanupScheduler.cleanupDeletedMessages());

            verify(messageRepository, times(1)).deleteOldDeletedMessages(any(Instant.class));
        }

        @Test
        @DisplayName("Вызывает метод репозитория только один раз")
        void cleanupDeletedMessages_ShouldCallRepositoryOnce() {
            doNothing().when(messageRepository).deleteOldDeletedMessages(any(Instant.class));

            messagesCleanupScheduler.cleanupDeletedMessages();
            
            verify(messageRepository, times(1)).deleteOldDeletedMessages(any(Instant.class));
            verifyNoMoreInteractions(messageRepository);
        }
    }

    @Nested
    @DisplayName("Проверка расчета даты")
    class DateCalculationTests {
        @Test
        @DisplayName("Расчет 30 дней в миллисекундах корректный")
        void thirtyDaysInMillisCalculation_ShouldBeCorrect() {
            long expectedMillis = 30L * 24L * 60L * 60L * 1000L;
            long actualMillis = TimeUnit.DAYS.toMillis(30);

            assertEquals(expectedMillis, actualMillis);
        }

        @Test
        @DisplayName("Дата отсечения должна быть в прошлом")
        void cutoffDate_ShouldBeInThePast() {
            doNothing().when(messageRepository).deleteOldDeletedMessages(any(Instant.class));

            messagesCleanupScheduler.cleanupDeletedMessages();
            
            verify(messageRepository).deleteOldDeletedMessages(argThat(cutoffDate -> {
                return cutoffDate.isBefore(Instant.now());
            }));
        }

        @Test
        @DisplayName("Дата отсечения примерно 30 дней назад")
        void cutoffDate_ShouldBeApproximately30DaysAgo() {
            doNothing().when(messageRepository).deleteOldDeletedMessages(any(Instant.class));
            Instant now = Instant.now();
            long thirtyDaysInMillis = TimeUnit.DAYS.toMillis(30);
            
            messagesCleanupScheduler.cleanupDeletedMessages();
            
            verify(messageRepository).deleteOldDeletedMessages(argThat(cutoffDate -> {
                long diff = now.toEpochMilli() - cutoffDate.toEpochMilli();
                return Math.abs(diff - thirtyDaysInMillis) < 1000;
            }));
        }
    }

}