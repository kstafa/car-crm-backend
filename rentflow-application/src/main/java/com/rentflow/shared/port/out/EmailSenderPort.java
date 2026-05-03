package com.rentflow.shared.port.out;

public interface EmailSenderPort {
    void send(String to, String subject, String htmlBody);

    void sendAsync(String to, String subject, String htmlBody);
}
