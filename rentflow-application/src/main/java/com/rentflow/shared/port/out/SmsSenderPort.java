package com.rentflow.shared.port.out;

public interface SmsSenderPort {
    void send(String phone, String message);
}
