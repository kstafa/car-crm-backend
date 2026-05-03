package com.rentflow.scheduler.service;

import com.rentflow.contract.ContractStatus;
import com.rentflow.contract.model.ContractSummary;
import com.rentflow.contract.port.out.ContractRepository;
import com.rentflow.customer.model.DocumentComplianceSummary;
import com.rentflow.customer.port.out.CustomerRepository;
import com.rentflow.notification.command.SendNotificationCommand;
import com.rentflow.notification.port.in.SendNotificationUseCase;
import com.rentflow.payment.InvoiceStatus;
import com.rentflow.payment.model.InvoiceSummary;
import com.rentflow.payment.port.out.InvoiceRepository;
import com.rentflow.shared.AuditEntry;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.InvoiceId;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.VehicleId;
import com.rentflow.shared.money.Money;
import com.rentflow.shared.port.out.AuditLogPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduledTaskApplicationServiceTest {

    @Mock ContractRepository contractRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock CustomerRepository customerRepository;
    @Mock SendNotificationUseCase sendNotification;
    @Mock AuditLogPort auditLog;
    ScheduledTaskApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ScheduledTaskApplicationService(contractRepository, invoiceRepository, customerRepository,
                sendNotification, auditLog);
    }

    @Test
    void markOverdueReturns_withOverdueContracts_sendsNotificationForEach() {
        when(contractRepository.findOverdue()).thenReturn(List.of(contract("C-1"), contract("C-2")));

        service.markOverdueReturns();

        verify(sendNotification, org.mockito.Mockito.times(2)).send(any(SendNotificationCommand.class));
    }

    @Test
    void markOverdueReturns_noOverdueContracts_sendsNoNotifications() {
        when(contractRepository.findOverdue()).thenReturn(List.of());

        service.markOverdueReturns();

        verify(sendNotification, never()).send(any());
    }

    @Test
    void markOverdueReturns_auditsExecution() {
        when(contractRepository.findOverdue()).thenReturn(List.of());

        service.markOverdueReturns();

        verify(auditLog).log(argThat(entry -> entry.actionType().equals("OVERDUE_CHECK_EXECUTED")));
    }

    @Test
    void checkExpiringDocuments_withExpiringDocs_sendsNotificationPerDocument() {
        when(customerRepository.findExpiringDocuments(30)).thenReturn(List.of(document(), document()));

        service.checkExpiringDocuments();

        verify(sendNotification, org.mockito.Mockito.times(2)).send(any(SendNotificationCommand.class));
    }

    @Test
    void checkExpiringDocuments_noExpiringDocs_sendsNoNotifications() {
        when(customerRepository.findExpiringDocuments(30)).thenReturn(List.of());

        service.checkExpiringDocuments();

        verify(sendNotification, never()).send(any());
    }

    @Test
    void markOverdueInvoices_withOverdueInvoices_auditsCount() {
        when(invoiceRepository.findOverdue()).thenReturn(List.of(invoice(), invoice()));

        service.markOverdueInvoices();

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditLog).log(captor.capture());
        assertEquals("2", captor.getValue().entityId());
    }

    @Test
    void markOverdueInvoices_noOverdueInvoices_audits() {
        when(invoiceRepository.findOverdue()).thenReturn(List.of());

        service.markOverdueInvoices();

        verify(auditLog).log(argThat(entry -> entry.actionType().equals("OVERDUE_INVOICES_CHECK_EXECUTED")));
    }

    private static ContractSummary contract(String number) {
        return new ContractSummary(ContractId.generate(), number, ReservationId.generate(), CustomerId.generate(),
                "Ada Lovelace", VehicleId.generate(), "AA-001",
                ZonedDateTime.now(ZoneOffset.UTC).minusDays(2),
                ZonedDateTime.now(ZoneOffset.UTC).minusDays(1),
                null, null, ContractStatus.ACTIVE);
    }

    private static DocumentComplianceSummary document() {
        return new DocumentComplianceSummary(CustomerId.generate(), "Ada Lovelace", "ada@example.com",
                "PASSPORT", LocalDate.now().plusDays(10), 10);
    }

    private static InvoiceSummary invoice() {
        Money zero = Money.zero(Currency.getInstance("EUR"));
        return new InvoiceSummary(InvoiceId.generate(), "I-1", ContractId.generate(), CustomerId.generate(),
                InvoiceStatus.SENT, zero, zero, zero, LocalDate.now().minusDays(10),
                LocalDate.now().minusDays(1));
    }
}
