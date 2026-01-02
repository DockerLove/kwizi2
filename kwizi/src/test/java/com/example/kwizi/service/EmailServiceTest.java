package com.example.kwizi.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Тесты")
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
        ReflectionTestUtils.setField(emailService, "fromEmail", FROM_EMAIL);
        ReflectionTestUtils.setField(emailService, "verificationBaseUrl", BASE_URL);
    }

    @Nested
    @DisplayName("Основные сценарии")
    class MainScenarios {

        @Test
        @DisplayName("Успешная отправка email верификации")
        void sendVerificationEmailAsync_WithValidParameters_ShouldSendEmail() {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            emailService.sendVerificationEmailAsync(TO_EMAIL, VERIFICATION_TOKEN);

            verify(mailSender).send(mimeMessageCaptor.capture());
            verify(mailSender).send(any(MimeMessage.class));
        }
    }

    @Nested
    @DisplayName("Валидация параметров")
    class ValidationTests {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "  ", "\t"})
        @DisplayName("Некорректный email адрес")
        void sendVerificationEmailAsync_WithInvalidEmail_ShouldThrowException(String invalidEmail) {
            assertThatThrownBy(() -> emailService.sendVerificationEmailAsync(invalidEmail, VERIFICATION_TOKEN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Email адрес получателя не может быть пустым");

            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "  ", "\t"})
        @DisplayName("Некорректный токен верификации")
        void sendVerificationEmailAsync_WithInvalidToken_ShouldThrowException(String invalidToken) {
            assertThatThrownBy(() -> emailService.sendVerificationEmailAsync(TO_EMAIL, invalidToken))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Токен верификации не может быть пустым");

            verify(mailSender, never()).send(any(MimeMessage.class));
        }
    }

    @Nested
    @DisplayName("Обработка ошибок")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Ошибка при отправке email")
        void sendVerificationEmailAsync_WhenMailSendingFails_ShouldThrowRuntimeException() {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doThrow(new MailException("SMTP error") {}).when(mailSender).send(any(MimeMessage.class));

            assertThatThrownBy(() -> emailService.sendVerificationEmailAsync(TO_EMAIL, VERIFICATION_TOKEN))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Не удалось отправить email верификации")
                    .hasCauseInstanceOf(MailException.class);
        }
    }

    @Nested
    @DisplayName("Форматирование HTML контента")
    class HtmlContentTests {

        @Test
        @DisplayName("Генерация валидного HTML с ссылкой верификации")
        void createEmailHtmlContent_ShouldGenerateValidHtmlWithVerificationLink() {
            String htmlContent = (String) ReflectionTestUtils.invokeMethod(
                    emailService, "createEmailHtmlContent", VERIFICATION_TOKEN
            );

            assertThat(htmlContent)
                    .isNotNull()
                    .contains("<!DOCTYPE html>")
                    .contains("<html lang=\"ru\">")
                    .contains("Подтверждение Email")
                    .contains("https://example.com/api/email-verification/verify-email?token=test-verification-token-123")
                    .contains("Команда Authentication Service");
        }

        @Test
        @DisplayName("Токен включается в ссылку верификации")
        void createEmailHtmlContent_WithDifferentTokens_ShouldIncludeTokenInLink() {
            String token1 = "token-one";
            String token2 = "token-two";

            String html1 = (String) ReflectionTestUtils.invokeMethod(
                    emailService, "createEmailHtmlContent", token1
            );
            String html2 = (String) ReflectionTestUtils.invokeMethod(
                    emailService, "createEmailHtmlContent", token2
            );

            assertThat(html1).contains("token=token-one");
            assertThat(html2).contains("token=token-two");
            assertThat(html1).isNotEqualTo(html2);
        }
    }

    @Nested
    @DisplayName("Работа с конфигурацией")
    class ConfigurationTests {

        @Test
        @DisplayName("Использование настроенных свойств")
        void emailService_ShouldUseConfiguredProperties() {
            String customFromEmail = "custom@example.com";
            String customBaseUrl = "https://custom.example.com";

            ReflectionTestUtils.setField(emailService, "fromEmail", customFromEmail);
            ReflectionTestUtils.setField(emailService, "verificationBaseUrl", customBaseUrl);

            String htmlContent = (String) ReflectionTestUtils.invokeMethod(
                    emailService, "createEmailHtmlContent", VERIFICATION_TOKEN
            );

            assertThat(htmlContent).contains(customBaseUrl + "/api/email-verification/verify-email?token=" + VERIFICATION_TOKEN);
        }
    }

    @Nested
    @DisplayName("Работа с почтовым клиентом")
    class MailClientTests {

        @Test
        @DisplayName("Создание и отправка MimeMessage")
        void sendVerificationEmailAsync_ShouldCreateAndSendMimeMessage() {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            emailService.sendVerificationEmailAsync(TO_EMAIL, VERIFICATION_TOKEN);

            verify(mailSender).createMimeMessage();
            verify(mailSender).send(any(MimeMessage.class));
        }
    }

    @Nested
    @DisplayName("Обработка разных форматов email")
    class EmailFormatsTests {

        @Test
        @DisplayName("Корректная обработка разных форматов email")
        void sendVerificationEmailAsync_WithDifferentEmailFormats_ShouldHandleCorrectly() {
            String[] validEmails = {
                    "user@example.com",
                    "user.name@example.com",
                    "user+tag@example.com",
                    "user@sub.domain.com"
            };

            for (String email : validEmails) {
                when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

                emailService.sendVerificationEmailAsync(email, VERIFICATION_TOKEN);
                verify(mailSender).send(any(MimeMessage.class));

                reset(mailSender);
            }
        }
    }
}