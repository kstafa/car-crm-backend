package com.rentflow.scheduler.service;

import com.rentflow.scheduler.port.in.CheckMaintenanceDueUseCase;
import org.springframework.stereotype.Service;

@Service
class CheckMaintenanceDueService implements CheckMaintenanceDueUseCase {
    private final ScheduledTaskApplicationService service;

    CheckMaintenanceDueService(ScheduledTaskApplicationService service) {
        this.service = service;
    }

    @Override
    public void execute() {
        service.checkMaintenanceDue();
    }
}
