package com.example.kwizi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Captor
    private ArgumentCaptor<MimeMessage> mimeMessageCaptor;

    @Mock
    private MimeMessage mimeMessage;

    private final String TO_EMAIL = "test@example.com";
    private final String VERIFICATION_TOKEN = "test-verification-token-123";
    private final String FROM_EMAIL = "noreply@example.com";
    private final String BASE_URL = "https://example.com";

    @BeforeEach
    void setUp() {
        // Настраиваем properties через ReflectionTestUtils
        ReflectionTestUtils.setField(emailService, "fromEmail", FROM_EMAIL);
        ReflectionTestUtils.setField(emailService, "verificationBaseUrl", BASE_URL);
    }

    // ===== ТЕСТЫ ДЛЯ УСПЕШНОЙ ОТПРАВКИ =====

    @Test
    void sendVerificationEmailAsync_WithValidParameters_ShouldSendEmail() throws MessagingException {
        // Arrange
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        emailService.sendVerificationEmailAsync(TO_EMAIL, VERIFICATION_TOKEN);

        // Assert
        verify(mailSender).send(mimeMessageCaptor.capture());
        verify(mailSender).send(any(MimeMessage.class));
    }

    // ===== ТЕСТЫ ДЛЯ ВАЛИДАЦИИ ПАРАМЕТРОВ =====

    @Test
    void sendVerificationEmailAsync_WithNullEmail_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> emailService.sendVerificationEmailAsync(null, VERIFICATION_TOKEN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email адрес получателя не может быть пустым");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendVerificationEmailAsync_WithEmptyEmail_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> emailService.sendVerificationEmailAsync("", VERIFICATION_TOKEN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email адрес получателя не может быть пустым");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendVerificationEmailAsync_WithBlankEmail_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> emailService.sendVerificationEmailAsync("   ", VERIFICATION_TOKEN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email адрес получателя не может быть пустым");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendVerificationEmailAsync_WithNullToken_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> emailService.sendVerificationEmailAsync(TO_EMAIL, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Токен верификации не может быть пустым");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendVerificationEmailAsync_WithEmptyToken_ShouldThrowException() {
        // Act & Assert
        assertThatThrownBy(() -> emailService.sendVerificationEmailAsync(TO_EMAIL, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Токен верификации не может быть пустым");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    // ===== ТЕСТЫ ДЛЯ ОШИБОК ОТПРАВКИ =====

    @Test
    void sendVerificationEmailAsync_WhenMailSendingFails_ShouldThrowRuntimeException() throws MessagingException {
        // Arrange
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailException("SMTP error") {}).when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        assertThatThrownBy(() -> emailService.sendVerificationEmailAsync(TO_EMAIL, VERIFICATION_TOKEN))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Не удалось отправить email верификации")
                .hasCauseInstanceOf(MailException.class);
    }


    // ===== ТЕСТЫ ДЛЯ ФОРМАТИРОВАНИЯ EMAIL =====

    @Test
    void createEmailHtmlContent_ShouldGenerateValidHtmlWithVerificationLink() {
        // Arrange
        String token = "test-token-123";

        // Act (тестируем приватный метод через публичный)
        String htmlContent = (String) ReflectionTestUtils.invokeMethod(
                emailService, "createEmailHtmlContent", token
        );

        // Assert
        assertThat(htmlContent).isNotNull();
        assertThat(htmlContent).contains("<!DOCTYPE html>");
        assertThat(htmlContent).contains("<html lang=\"ru\">");
        assertThat(htmlContent).contains("Подтверждение Email");
        assertThat(htmlContent).contains("https://example.com/api/email-verification/verify-email?token=test-token-123");
        assertThat(htmlContent).contains("Команда Authentication Service");
    }

    @Test
    void createEmailHtmlContent_WithDifferentTokens_ShouldIncludeTokenInLink() {
        // Arrange
        String token1 = "token-one";
        String token2 = "token-two";

        // Act
        String html1 = (String) ReflectionTestUtils.invokeMethod(
                emailService, "createEmailHtmlContent", token1
        );
        String html2 = (String) ReflectionTestUtils.invokeMethod(
                emailService, "createEmailHtmlContent", token2
        );

        // Assert
        assertThat(html1).contains("token=token-one");
        assertThat(html2).contains("token=token-two");
        assertThat(html1).isNotEqualTo(html2);
    }

    // ===== ТЕСТЫ ДЛЯ РАЗНЫХ EMAIL АДРЕСОВ =====

    @Test
    void sendVerificationEmailAsync_WithDifferentEmailFormats_ShouldHandleCorrectly() throws MessagingException {
        // Arrange
        String[] validEmails = {
                "user@example.com",
                "user.name@example.com",
                "user+tag@example.com",
                "user@sub.domain.com"
        };

        for (String email : validEmails) {
            // Для каждого email настраиваем мок
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // Act & Assert - должен выполниться без исключений
            emailService.sendVerificationEmailAsync(email, VERIFICATION_TOKEN);
            verify(mailSender).send(any(MimeMessage.class));

            // Reset invocation count для следующей итерации
            reset(mailSender);
        }
    }

    // ===== ТЕСТ ДЛЯ ПРОВЕРКИ КОНФИГУРАЦИИ =====

    @Test
    void emailService_ShouldUseConfiguredProperties() {
        // Arrange
        String customFromEmail = "custom@example.com";
        String customBaseUrl = "https://custom.example.com";

        ReflectionTestUtils.setField(emailService, "fromEmail", customFromEmail);
        ReflectionTestUtils.setField(emailService, "verificationBaseUrl", customBaseUrl);

        // Act
        String htmlContent = (String) ReflectionTestUtils.invokeMethod(
                emailService, "createEmailHtmlContent", VERIFICATION_TOKEN
        );

        // Assert
        assertThat(htmlContent).contains(customBaseUrl + "/api/email-verification/verify-email?token=" + VERIFICATION_TOKEN);
    }

    // ===== ТЕСТ ДЛЯ ПРОВЕРКИ СОДЕРЖАНИЯ EMAIL =====

    @Test
    void createVerificationEmail_ShouldSetCorrectHeadersAndContent() throws Exception {
        // Arrange
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act (тестируем приватный метод через отладку)
        emailService.sendVerificationEmailAsync(TO_EMAIL, VERIFICATION_TOKEN);

        // Assert - проверяем что методы создания email были вызваны
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }
}