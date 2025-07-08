package com.example.kwizi.service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private JavaMailSender mailSender;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async("taskExecutor")
    public void sendVerificationEmailAsync(String to, String token) {
        logger.info("Отправка письма для подтверждения email на: {}", to);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@example.com");
            message.setTo(to);
            message.setSubject("Подтверждение Email");
            message.setText(createEmailContent(token));
            mailSender.send(message);
            logger.info("Письмо для подтверждения email успешно отправлено на: {}", to);
        } catch (MailException e) {
            logger.error("Ошибка при отправке email на {}: {}", to, e.getMessage(), e); // Логируем ошибку, включая stack trace
            // Возможность добавить повторную попытку
        }
    }

    private String createEmailContent(String token) {
        String verificationLink = "http://localhost:8080/api/email-verification/verify-email?token=" + token;
        String content = String.format(
                "Здравствуйте!\n\nПожалуйста, перейдите по следующей ссылке, чтобы подтвердить свой email:\n%s\n\nС уважением,\nКоманда Authentication Service",
                verificationLink
        );
        logger.debug("Сгенерированный текст письма для подтверждения email: {}", content);
        return content;
    }
}