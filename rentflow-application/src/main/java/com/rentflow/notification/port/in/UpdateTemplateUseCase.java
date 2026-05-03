package com.rentflow.notification.port.in;

import com.rentflow.notification.command.UpdateTemplateCommand;

public interface UpdateTemplateUseCase {
    void update(UpdateTemplateCommand command);
}
