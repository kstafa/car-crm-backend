package com.rentflow.scheduler.service;

import com.rentflow.scheduler.port.in.MarkOverdueReturnsUseCase;
import org.springframework.stereotype.Service;

@Service
class MarkOverdueReturnsService implements MarkOverdueReturnsUseCase {
    private final ScheduledTaskApplicationService service;

    MarkOverdueReturnsService(ScheduledTaskApplicationService service) {
        this.service = service;
    }

    @Override
    public void execute() {
        service.markOverdueReturns();
    }
}
