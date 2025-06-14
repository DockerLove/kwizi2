package com.example.kwizi.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private JavaMailSender mailSender;
    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@example.com"); // Замените на ваш адрес отправителя
        message.setTo(to);
        message.setSubject("Подтверждение Email");
        message.setText("Здравствуйте!\n\nПожалуйста, перейдите по следующей ссылке, чтобы подтвердить свой email:\n" +
                "http://localhost:8080/api/email-verification/verify-email?token=" + token +
                "\n\nС уважением,\nКоманда Authentication Service");
        mailSender.send(message);
    }
}