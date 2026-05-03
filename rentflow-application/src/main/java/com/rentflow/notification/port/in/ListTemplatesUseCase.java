package com.rentflow.notification.port.in;

import com.rentflow.notification.model.NotificationTemplateSummary;

import java.util.List;

public interface ListTemplatesUseCase {
    List<NotificationTemplateSummary> list();
}
