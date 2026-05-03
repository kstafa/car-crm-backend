package com.rentflow.scheduler.service;

import com.rentflow.scheduler.port.in.MarkOverdueInvoicesUseCase;
import org.springframework.stereotype.Service;

@Service
class MarkOverdueInvoicesService implements MarkOverdueInvoicesUseCase {
    private final ScheduledTaskApplicationService service;

    MarkOverdueInvoicesService(ScheduledTaskApplicationService service) {
        this.service = service;
    }

    @Override
    public void execute() {
        service.markOverdueInvoices();
    }
}
