package com.rentflow.scheduler.adapter.in;

import com.rentflow.scheduler.port.in.CheckExpiringDocumentsUseCase;
import com.rentflow.scheduler.port.in.CheckMaintenanceDueUseCase;
import com.rentflow.scheduler.port.in.MarkOverdueInvoicesUseCase;
import com.rentflow.scheduler.port.in.MarkOverdueReturnsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RentFlowScheduler {

    private final MarkOverdueReturnsUseCase markOverdueReturns;
    private final CheckExpiringDocumentsUseCase checkExpiringDocuments;
    private final CheckMaintenanceDueUseCase checkMaintenanceDue;
    private final MarkOverdueInvoicesUseCase markOverdueInvoices;

    @Scheduled(cron = "0 0 8 * * *", zone = "UTC")
    public void runOverdueReturnsCheck() {
        log.info("[Scheduler] Running overdue returns check");
        markOverdueReturns.execute();
    }

    @Scheduled(cron = "0 0 9 * * *", zone = "UTC")
    public void runExpiringDocumentsCheck() {
        log.info("[Scheduler] Running expiring documents check");
        checkExpiringDocuments.execute();
    }

    @Scheduled(cron = "0 0 7 * * MON", zone = "UTC")
    public void runMaintenanceDueCheck() {
        log.info("[Scheduler] Running maintenance due check");
        checkMaintenanceDue.execute();
    }

    @Scheduled(cron = "0 30 8 * * *", zone = "UTC")
    public void runOverdueInvoicesCheck() {
        log.info("[Scheduler] Running overdue invoices check");
        markOverdueInvoices.execute();
    }
}
