package com.rentflow.notification.port.in;

import com.rentflow.notification.command.SendNotificationCommand;

public interface SendNotificationUseCase {
    void send(SendNotificationCommand command);
}
