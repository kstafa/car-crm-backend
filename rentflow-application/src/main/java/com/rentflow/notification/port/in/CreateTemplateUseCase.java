package com.rentflow.notification.port.in;

import com.rentflow.notification.NotificationTemplateId;
import com.rentflow.notification.command.CreateTemplateCommand;

public interface CreateTemplateUseCase {
    NotificationTemplateId create(CreateTemplateCommand command);
}
