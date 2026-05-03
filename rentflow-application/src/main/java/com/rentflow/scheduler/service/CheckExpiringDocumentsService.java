package com.rentflow.scheduler.service;

import com.rentflow.scheduler.port.in.CheckExpiringDocumentsUseCase;
import org.springframework.stereotype.Service;

@Service
class CheckExpiringDocumentsService implements CheckExpiringDocumentsUseCase {
    private final ScheduledTaskApplicationService service;

    CheckExpiringDocumentsService(ScheduledTaskApplicationService service) {
        this.service = service;
    }

    @Override
    public void execute() {
        service.checkExpiringDocuments();
    }
}
