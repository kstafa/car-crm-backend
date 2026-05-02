package com.rentflow.payment.service;

import com.rentflow.payment.Deposit;
import com.rentflow.payment.DepositId;
import com.rentflow.payment.DepositStatus;
import com.rentflow.payment.Invoice;
import com.rentflow.payment.InvoiceLineItemType;
import com.rentflow.payment.InvoicePaidEvent;
import com.rentflow.payment.InvoiceStatus;
import com.rentflow.payment.LineItem;
import com.rentflow.payment.PaymentMethod;
import com.rentflow.payment.Refund;
import com.rentflow.payment.RefundId;
import com.rentflow.payment.RefundReason;
import com.rentflow.payment.command.ApproveRefundCommand;
import com.rentflow.payment.command.CreateInvoiceForContractCommand;
import com.rentflow.payment.command.ForfeitDepositCommand;
import com.rentflow.payment.command.ProcessRefundCommand;
import com.rentflow.payment.command.RecordPaymentCommand;
import com.rentflow.payment.command.ReleaseDepositCommand;
import com.rentflow.payment.command.RequestRefundCommand;
import com.rentflow.payment.command.SendInvoiceCommand;
import com.rentflow.payment.command.VoidInvoiceCommand;
import com.rentflow.payment.model.InvoiceDetail;
import com.rentflow.payment.port.out.DepositRepository;
import com.rentflow.payment.port.out.InvoiceRepository;
import com.rentflow.payment.port.out.RefundRepository;
import com.rentflow.shared.DomainException;
import com.rentflow.shared.InvalidStateTransitionException;
import com.rentflow.shared.ResourceNotFoundException;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.InvoiceId;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.money.Money;
import com.rentflow.shared.port.out.AuditLogPort;
import com.rentflow.shared.port.out.DomainEventPublisher;
import com.rentflow.shared.port.out.PdfGeneratorPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentApplicationServiceTest {

    private static final Currency EUR = Currency.getInstance("EUR");

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private DepositRepository depositRepository;
    @Mock
    private RefundRepository refundRepository;
    @Mock
    private DomainEventPublisher eventPublisher;
    @Mock
    private AuditLogPort auditLog;
    @Mock
    private PdfGeneratorPort pdfGenerator;

    private PaymentApplicationService service;
    private StaffId staffId;

    @BeforeEach
    void setUp() {
        service = new PaymentApplicationService(invoiceRepository, depositRepository, refundRepository,
                eventPublisher, auditLog, pdfGenerator);
        staffId = StaffId.generate();
    }

    @Test
    void createForContract_standardRental_createsInvoiceWithCorrectLineItems() {
        service.createForContract(command(money("300.00"), money("0.00"), money("60.00"),
                money("0.00"), money("0.00"), money("0.00")));

        Invoice invoice = savedInvoice();
        assertEquals(2, invoice.getLineItems().size());
        assertEquals(InvoiceLineItemType.RENTAL_BASE, invoice.getLineItems().get(0).type());
        assertEquals(InvoiceLineItemType.TAX, invoice.getLineItems().get(1).type());
    }

    @Test
    void createForContract_withLateFee_includesLateFeeLineItem() {
        service.createForContract(command(money("300.00"), money("0.00"), money("0.00"),
                money("45.00"), money("0.00"), money("0.00")));

        assertTrue(savedInvoice().getLineItems().stream().anyMatch(i -> i.type() == InvoiceLineItemType.LATE_FEE));
    }

    @Test
    void createForContract_withFuelSurcharge_includesFuelSurchargeLineItem() {
        service.createForContract(command(money("300.00"), money("0.00"), money("0.00"),
                money("0.00"), money("62.50"), money("0.00")));

        assertTrue(savedInvoice().getLineItems().stream()
                .anyMatch(i -> i.type() == InvoiceLineItemType.FUEL_SURCHARGE));
    }

    @Test
    void createForContract_withDiscount_includesNegativeDiscountLineItem() {
        service.createForContract(command(money("300.00"), money("50.00"), money("0.00"),
                money("0.00"), money("0.00"), money("0.00")));

        LineItem discount = savedInvoice().getLineItems().stream()
                .filter(i -> i.type() == InvoiceLineItemType.DISCOUNT)
                .findFirst()
                .orElseThrow();
        assertEquals(new BigDecimal("-50.00"), discount.unitPrice().amount());
    }

    @Test
    void createForContract_zeroLateFeeAndSurcharge_excludesThoseLineItems() {
        service.createForContract(command(money("300.00"), money("0.00"), money("0.00"),
                money("0.00"), money("0.00"), money("0.00")));

        assertFalse(savedInvoice().getLineItems().stream().anyMatch(i -> i.type() == InvoiceLineItemType.LATE_FEE));
        assertFalse(savedInvoice().getLineItems().stream()
                .anyMatch(i -> i.type() == InvoiceLineItemType.FUEL_SURCHARGE));
    }

    @Test
    void createForContract_positiveDeposit_createsAndSavesDeposit() {
        service.createForContract(command(money("300.00"), money("0.00"), money("0.00"),
                money("0.00"), money("0.00"), money("300.00")));

        ArgumentCaptor<Deposit> captor = ArgumentCaptor.forClass(Deposit.class);
        verify(depositRepository).save(captor.capture());
        assertEquals(DepositStatus.HELD, captor.getValue().getStatus());
    }

    @Test
    void createForContract_zeroDeposit_doesNotCreateDeposit() {
        service.createForContract(command(money("300.00"), money("0.00"), money("0.00"),
                money("0.00"), money("0.00"), money("0.00")));

        verify(depositRepository, never()).save(any());
    }

    @Test
    void createForContract_invoiceSentImmediately_statusIsSent() {
        service.createForContract(command(money("300.00"), money("0.00"), money("0.00"),
                money("0.00"), money("0.00"), money("0.00")));

        assertEquals(InvoiceStatus.SENT, savedInvoice().getStatus());
    }

    @Test
    void send_draftInvoice_savesWithSentStatus() {
        Invoice invoice = invoice();
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        service.send(new SendInvoiceCommand(invoice.getId(), staffId));

        assertEquals(InvoiceStatus.SENT, invoice.getStatus());
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void send_notDraft_throwsInvalidStateTransition() {
        Invoice invoice = invoice();
        invoice.send();
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        assertThrows(InvalidStateTransitionException.class,
                () -> service.send(new SendInvoiceCommand(invoice.getId(), staffId)));
    }

    @Test
    void send_notFound_throwsResourceNotFoundException() {
        InvoiceId id = InvoiceId.generate();
        when(invoiceRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.send(new SendInvoiceCommand(id, staffId)));
    }

    @Test
    void voidInvoice_noPriorPayments_savesWithVoidedStatus() {
        Invoice invoice = invoice();
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        service.voidInvoice(new VoidInvoiceCommand(invoice.getId(), staffId));

        assertEquals(InvoiceStatus.VOIDED, invoice.getStatus());
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void voidInvoice_withPriorPayments_throwsDomainException() {
        Invoice invoice = invoice();
        invoice.recordPayment(money("1.00"), PaymentMethod.CASH, null);
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        assertThrows(DomainException.class, () -> service.voidInvoice(new VoidInvoiceCommand(invoice.getId(),
                staffId)));
    }

    @Test
    void recordPayment_partialAmount_updatesOutstandingBalance() {
        Invoice invoice = invoice();
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        service.record(new RecordPaymentCommand(invoice.getId(), money("50.00"), PaymentMethod.CARD, "ref",
                staffId));

        assertEquals(money("250.00"), invoice.outstandingAmount());
    }

    @Test
    void recordPayment_fullAmount_triggersInvoicePaidEvent() {
        Invoice invoice = invoice();
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        service.record(new RecordPaymentCommand(invoice.getId(), money("300.00"), PaymentMethod.CARD, "ref",
                staffId));

        verify(eventPublisher).publish(any(InvoicePaidEvent.class));
    }

    @Test
    void recordPayment_exceedsOutstanding_throwsDomainException() {
        Invoice invoice = invoice();
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        assertThrows(DomainException.class, () -> service.record(new RecordPaymentCommand(invoice.getId(),
                money("300.01"), PaymentMethod.CARD, "ref", staffId)));
    }

    @Test
    void recordPayment_notFound_throwsResourceNotFoundException() {
        InvoiceId id = InvoiceId.generate();
        when(invoiceRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.record(new RecordPaymentCommand(id,
                money("1.00"), PaymentMethod.CARD, "ref", staffId)));
    }

    @Test
    void generatePdf_existingInvoice_delegatesToPdfPort() {
        Invoice invoice = invoice();
        byte[] expected = new byte[]{'%', 'P', 'D', 'F'};
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        when(pdfGenerator.generateInvoice(any(InvoiceDetail.class))).thenReturn(expected);

        byte[] actual = service.generate(invoice.getId());

        assertArrayEquals(expected, actual);
    }

    @Test
    void release_heldDeposit_savesReleasedAndPublishesEvent() {
        Deposit deposit = deposit();
        deposit.pullDomainEvents();
        when(depositRepository.findById(deposit.getId())).thenReturn(Optional.of(deposit));

        service.release(new ReleaseDepositCommand(deposit.getId(), "completed", staffId));

        assertEquals(DepositStatus.RELEASED, deposit.getStatus());
        verify(eventPublisher).publish(any());
    }

    @Test
    void release_notHeld_throwsInvalidStateTransition() {
        Deposit deposit = deposit();
        deposit.release("completed");
        when(depositRepository.findById(deposit.getId())).thenReturn(Optional.of(deposit));

        assertThrows(InvalidStateTransitionException.class,
                () -> service.release(new ReleaseDepositCommand(deposit.getId(), "again", staffId)));
    }

    @Test
    void release_notFound_throwsResourceNotFoundException() {
        DepositId id = DepositId.generate();
        when(depositRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.release(new ReleaseDepositCommand(id, "completed", staffId)));
    }

    @Test
    void forfeit_heldDeposit_savesForfeitedAndPublishesEvent() {
        Deposit deposit = deposit();
        deposit.pullDomainEvents();
        when(depositRepository.findById(deposit.getId())).thenReturn(Optional.of(deposit));

        service.forfeit(new ForfeitDepositCommand(deposit.getId(), "damage", staffId));

        assertEquals(DepositStatus.FORFEITED, deposit.getStatus());
        verify(eventPublisher).publish(any());
    }

    @Test
    void forfeit_notHeld_throwsInvalidStateTransition() {
        Deposit deposit = deposit();
        deposit.forfeit("damage");
        when(depositRepository.findById(deposit.getId())).thenReturn(Optional.of(deposit));

        assertThrows(InvalidStateTransitionException.class,
                () -> service.forfeit(new ForfeitDepositCommand(deposit.getId(), "again", staffId)));
    }

    @Test
    void requestRefund_paidInvoice_savesAndReturnsId() {
        Invoice invoice = invoice();
        invoice.recordPayment(money("300.00"), PaymentMethod.CARD, null);
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        RefundId id = service.request(new RequestRefundCommand(invoice.getId(), money("100.00"),
                RefundReason.GOODWILL, PaymentMethod.CARD, "note", staffId));

        assertNotNull(id);
        verify(refundRepository).save(any(Refund.class));
    }

    @Test
    void requestRefund_unpaidInvoice_throwsDomainException() {
        Invoice invoice = invoice();
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        assertThrows(DomainException.class, () -> service.request(new RequestRefundCommand(invoice.getId(),
                money("1.00"), RefundReason.GOODWILL, PaymentMethod.CARD, "note", staffId)));
    }

    @Test
    void requestRefund_amountExceedsPaid_throwsDomainException() {
        Invoice invoice = invoice();
        invoice.recordPayment(money("100.00"), PaymentMethod.CARD, null);
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        assertThrows(DomainException.class, () -> service.request(new RequestRefundCommand(invoice.getId(),
                money("100.01"), RefundReason.GOODWILL, PaymentMethod.CARD, "note", staffId)));
    }

    @Test
    void approveRefund_pendingRefund_savesApproved() {
        Refund refund = refund();
        when(refundRepository.findById(refund.getId())).thenReturn(Optional.of(refund));

        service.approve(new ApproveRefundCommand(refund.getId(), staffId));

        assertEquals(com.rentflow.payment.RefundStatus.APPROVED, refund.getStatus());
        verify(refundRepository).save(refund);
    }

    @Test
    void approveRefund_notPending_throwsInvalidStateTransition() {
        Refund refund = refund();
        refund.approve(staffId);
        when(refundRepository.findById(refund.getId())).thenReturn(Optional.of(refund));

        assertThrows(InvalidStateTransitionException.class,
                () -> service.approve(new ApproveRefundCommand(refund.getId(), staffId)));
    }

    @Test
    void processRefund_approvedRefund_savesProcessed() {
        Refund refund = refund();
        refund.approve(staffId);
        when(refundRepository.findById(refund.getId())).thenReturn(Optional.of(refund));

        service.process(new ProcessRefundCommand(refund.getId(), staffId));

        assertEquals(com.rentflow.payment.RefundStatus.PROCESSED, refund.getStatus());
        verify(refundRepository).save(refund);
    }

    @Test
    void processRefund_notApproved_throwsInvalidStateTransition() {
        Refund refund = refund();
        when(refundRepository.findById(refund.getId())).thenReturn(Optional.of(refund));

        assertThrows(InvalidStateTransitionException.class,
                () -> service.process(new ProcessRefundCommand(refund.getId(), staffId)));
    }

    private Invoice savedInvoice() {
        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(captor.capture());
        return captor.getValue();
    }

    private static CreateInvoiceForContractCommand command(Money base, Money discount, Money tax, Money late,
                                                           Money fuel, Money deposit) {
        return new CreateInvoiceForContractCommand(ContractId.generate(), CustomerId.generate(),
                ReservationId.generate(), base, discount, tax, late, fuel, deposit, EUR, 3);
    }

    private static Invoice invoice() {
        return Invoice.create(ContractId.generate(), CustomerId.generate(),
                List.of(new LineItem("rental", InvoiceLineItemType.RENTAL_BASE, money("300.00"), 1)),
                LocalDate.now().plusDays(7));
    }

    private static Deposit deposit() {
        return Deposit.hold(ContractId.generate(), CustomerId.generate(), InvoiceId.generate(), money("300.00"));
    }

    private static Refund refund() {
        return Refund.request(InvoiceId.generate(), CustomerId.generate(), money("100.00"),
                RefundReason.GOODWILL, PaymentMethod.CARD, null);
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), EUR);
    }
}
