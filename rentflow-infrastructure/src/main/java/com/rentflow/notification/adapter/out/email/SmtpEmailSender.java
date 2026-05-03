package com.rentflow.notification.adapter.out.email;

import com.rentflow.shared.InfrastructureException;
import com.rentflow.shared.port.out.EmailSenderPort;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rentflow.email.enabled", havingValue = "true")
public class SmtpEmailSender implements EmailSenderPort {

    private final JavaMailSender mailSender;

    @Value("${rentflow.email.from:noreply@rentflow.com}")
    private String fromAddress;

    @Override
    public void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject != null ? subject : "RentFlow Notification");
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new InfrastructureException("Failed to send email to " + to, e);
        }
    }

    @Override
    @Async
    public void sendAsync(String to, String subject, String htmlBody) {
        send(to, subject, htmlBody);
    }
}
