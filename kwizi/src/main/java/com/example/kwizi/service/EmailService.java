package com.example.kwizi.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.verification.email.from}")
    private String fromEmail;

    @Value("${app.verification.base-url}")
    private String verificationBaseUrl;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async("taskExecutor")
    public void sendVerificationEmailAsync(String to, String token) {
        validateEmailParameters(to, token);

        logger.info("Отправка письма для подтверждения email на: {}", to);
        try {
            MimeMessage emailMessage = createVerificationEmail(to, token);
            mailSender.send(emailMessage);
            logger.info("Письмо для подтверждения email успешно отправлено на: {}", to);
        } catch (MailException | MessagingException e) {
            throw new RuntimeException("Не удалось отправить email верификации", e);
        }
    }

    private void validateEmailParameters(String to, String token) {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Email адрес получателя не может быть пустым");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Токен верификации не может быть пустым");
        }
    }

    private MimeMessage createVerificationEmail(String to, String token) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("Подтверждение Email");

        String htmlContent = createEmailHtmlContent(token);
        helper.setText(htmlContent, true);

        return mimeMessage;
    }

    private String createEmailHtmlContent(String token) {
        String verificationLink = verificationBaseUrl + "/api/email-verification/verify-email?token=" + token;

        return String.format(
                """
                <!DOCTYPE html>
                <html lang="ru">
                <head>
                    <meta charset="UTF-8">
                    <title>Подтверждение Email</title>
                </head>
                <body>
                    <p>Здравствуйте!</p>
                    <p>Пожалуйста, перейдите по следующей ссылке, чтобы подтвердить свой email:</p>
                    <p><a href="%s">Подтвердить Email</a></p>
                    <p>Если ссылка не работает, скопируйте и вставьте в браузер следующую ссылку:<br>%s</p>
                    <p>С уважением,<br>Команда Authentication Service</p>
                </body>
                </html>
                """,
                verificationLink, verificationLink
        );
    }
}