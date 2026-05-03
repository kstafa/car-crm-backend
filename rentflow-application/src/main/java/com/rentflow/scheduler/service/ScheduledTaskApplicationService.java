package com.rentflow.scheduler.service;

import com.rentflow.contract.model.ContractSummary;
import com.rentflow.contract.port.out.ContractRepository;
import com.rentflow.customer.model.DocumentComplianceSummary;
import com.rentflow.customer.port.out.CustomerRepository;
import com.rentflow.notification.NotificationTrigger;
import com.rentflow.notification.command.SendNotificationCommand;
import com.rentflow.notification.port.in.SendNotificationUseCase;
import com.rentflow.payment.model.InvoiceSummary;
import com.rentflow.payment.port.out.InvoiceRepository;
import com.rentflow.shared.AuditEntry;
import com.rentflow.shared.port.out.AuditLogPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ScheduledTaskApplicationService {

    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final SendNotificationUseCase sendNotification;
    private final AuditLogPort auditLog;

    public ScheduledTaskApplicationService(ContractRepository contractRepository, InvoiceRepository invoiceRepository,
                                           CustomerRepository customerRepository,
                                           SendNotificationUseCase sendNotification, AuditLogPort auditLog) {
        this.contractRepository = contractRepository;
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.sendNotification = sendNotification;
        this.auditLog = auditLog;
    }

    public void markOverdueReturns() {
        List<ContractSummary> overdue = contractRepository.findOverdue();
        for (ContractSummary summary : overdue) {
            sendNotification.send(new SendNotificationCommand(
                    NotificationTrigger.RETURN_OVERDUE,
                    Map.of(
                            "contractNumber", summary.contractNumber(),
                            "vehicleLicensePlate", summary.vehicleLicensePlate() == null
                                    ? "" : summary.vehicleLicensePlate(),
                            "scheduledReturn", summary.scheduledReturn().toString()),
                    null, null, null,
                    "Contract", summary.id().value().toString()));
        }
        auditLog.log(AuditEntry.of("OVERDUE_CHECK_EXECUTED", "ScheduledTask", null, "SYSTEM"));
    }

    public void checkExpiringDocuments() {
        List<DocumentComplianceSummary> expiring = customerRepository.findExpiringDocuments(30);
        for (DocumentComplianceSummary doc : expiring) {
            sendNotification.send(new SendNotificationCommand(
                    NotificationTrigger.DOCUMENT_EXPIRING,
                    Map.of(
                            "customerName", doc.customerName(),
                            "documentType", doc.documentType(),
                            "expiryDate", doc.expiryDate().toString(),
                            "daysRemaining", String.valueOf(doc.daysRemaining())),
                    doc.customerEmail(), null, null,
                    "Customer", doc.customerId().value().toString()));
        }
    }

    public void checkMaintenanceDue() {
        auditLog.log(AuditEntry.of("MAINTENANCE_CHECK_EXECUTED", "ScheduledTask", null, "SYSTEM"));
    }

    public void markOverdueInvoices() {
        List<InvoiceSummary> overdue = invoiceRepository.findOverdue();
        if (!overdue.isEmpty()) {
            auditLog.log(AuditEntry.of("OVERDUE_INVOICES_DETECTED",
                    "Invoice", String.valueOf(overdue.size()), "SYSTEM"));
        } else {
            auditLog.log(AuditEntry.of("OVERDUE_INVOICES_CHECK_EXECUTED",
                    "Invoice", "0", "SYSTEM"));
        }
    }
}
