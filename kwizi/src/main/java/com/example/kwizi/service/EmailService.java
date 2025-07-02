package com.example.kwizi.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private JavaMailSender mailSender;
    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    @Async("taskExecutor")
    public void sendVerificationEmailAsync(String to, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@example.com");
            message.setTo(to);
            message.setSubject("Подтверждение Email");
            message.setText(createEmailContent(token));
            mailSender.send(message);
        } catch (MailException e) {
            // Логирование ошибки
            System.err.println("Ошибка при отправке email: " + e.getMessage());
            // Возможность добавить повторную попытку
        }
    }

    private String createEmailContent(String token) {
        return "Здравствуйте!\n\nПожалуйста, перейдите по следующей ссылке, чтобы подтвердить свой email:\n" +
                "http://localhost:8080/api/email-verification/verify-email?token=" + token +
                "\n\nС уважением,\nКоманда Authentication Service";
    }
}