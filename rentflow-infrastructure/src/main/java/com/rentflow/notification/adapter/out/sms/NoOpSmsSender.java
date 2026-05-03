package com.rentflow.notification.adapter.out.sms;

import com.rentflow.shared.port.out.SmsSenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NoOpSmsSender implements SmsSenderPort {
    @Override
    public void send(String phone, String message) {
        log.info("[NoOp SMS] phone={} message={}", phone, message);
    }
}
