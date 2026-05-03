package com.rentflow.notification.service;

import com.rentflow.notification.model.NotificationTemplateSummary;
import com.rentflow.notification.port.in.ListTemplatesUseCase;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class ListTemplatesService implements ListTemplatesUseCase {
    private final NotificationApplicationService service;

    ListTemplatesService(NotificationApplicationService service) {
        this.service = service;
    }

    @Override
    public List<NotificationTemplateSummary> list() {
        return service.listTemplates();
    }
}
