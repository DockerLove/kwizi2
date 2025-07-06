package com.example.kwizi.service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendVerificationEmailAsync_ShouldSendCorrectEmail() {
        // Arrange
        String testEmail = "user@example.com";
        String testToken = "test-verification-token";
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        // Act
        emailService.sendVerificationEmailAsync(testEmail, testToken);

        // Assert
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sentMessage = messageCaptor.getValue();

        assertAll(
                () -> assertEquals("noreply@example.com", sentMessage.getFrom()),
                () -> assertEquals(testEmail, sentMessage.getTo()[0]),
                () -> assertEquals("Подтверждение Email", sentMessage.getSubject()),
                () -> assertTrue(sentMessage.getText().contains(testToken)),
                () -> assertTrue(sentMessage.getText().contains("http://localhost:8080/api/email-verification/verify-email"))
        );
    }

    @Test
    void sendVerificationEmailAsync_WhenMailException_ShouldLogError() {
        // Arrange
        String testEmail = "user@example.com";
        String testToken = "test-token";

        doThrow(new MailException("SMTP error") {})
                .when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        emailService.sendVerificationEmailAsync(testEmail, testToken);

        // Assert
        verify(mailSender).send(any(SimpleMailMessage.class));
        // Здесь можно добавить проверку логов, если используется Logger
    }

}