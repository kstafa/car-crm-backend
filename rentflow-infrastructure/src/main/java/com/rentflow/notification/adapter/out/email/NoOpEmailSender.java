package com.rentflow.notification.adapter.out.email;

import com.rentflow.shared.port.out.EmailSenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "rentflow.email.enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class NoOpEmailSender implements EmailSenderPort {
    @Override
    public void send(String to, String subject, String htmlBody) {
        log.info("[NoOp Email] to={} subject={}", to, subject);
    }

    @Override
    @Async
    public void sendAsync(String to, String subject, String htmlBody) {
        log.info("[NoOp Email Async] to={} subject={}", to, subject);
    }
}
