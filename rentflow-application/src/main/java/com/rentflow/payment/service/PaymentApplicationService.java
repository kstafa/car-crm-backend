package com.rentflow.payment.service;

import com.rentflow.payment.Deposit;
import com.rentflow.payment.DepositId;
import com.rentflow.payment.Invoice;
import com.rentflow.payment.InvoiceLineItemType;
import com.rentflow.payment.InvoiceStatus;
import com.rentflow.payment.LineItem;
import com.rentflow.payment.Refund;
import com.rentflow.payment.RefundId;
import com.rentflow.payment.command.ApproveRefundCommand;
import com.rentflow.payment.command.CreateInvoiceForContractCommand;
import com.rentflow.payment.command.ForfeitDepositCommand;
import com.rentflow.payment.command.ProcessRefundCommand;
import com.rentflow.payment.command.RecordPaymentCommand;
import com.rentflow.payment.command.ReleaseDepositCommand;
import com.rentflow.payment.command.RequestRefundCommand;
import com.rentflow.payment.command.SendInvoiceCommand;
import com.rentflow.payment.command.VoidInvoiceCommand;
import com.rentflow.payment.model.DepositDetail;
import com.rentflow.payment.model.DepositSummary;
import com.rentflow.payment.model.InvoiceDetail;
import com.rentflow.payment.model.InvoiceSummary;
import com.rentflow.payment.model.RefundSummary;
import com.rentflow.payment.port.in.ApproveRefundUseCase;
import com.rentflow.payment.port.in.CreateInvoiceForContractUseCase;
import com.rentflow.payment.port.in.ForfeitDepositUseCase;
import com.rentflow.payment.port.in.GenerateInvoicePdfUseCase;
import com.rentflow.payment.port.in.GetDepositUseCase;
import com.rentflow.payment.port.in.GetInvoiceUseCase;
import com.rentflow.payment.port.in.ListDepositsUseCase;
import com.rentflow.payment.port.in.ListInvoicesUseCase;
import com.rentflow.payment.port.in.ListRefundsUseCase;
import com.rentflow.payment.port.in.ProcessRefundUseCase;
import com.rentflow.payment.port.in.RecordPaymentUseCase;
import com.rentflow.payment.port.in.ReleaseDepositUseCase;
import com.rentflow.payment.port.in.RequestRefundUseCase;
import com.rentflow.payment.port.in.SendInvoiceUseCase;
import com.rentflow.payment.port.in.VoidInvoiceUseCase;
import com.rentflow.payment.port.out.DepositRepository;
import com.rentflow.payment.port.out.InvoiceRepository;
import com.rentflow.payment.port.out.RefundRepository;
import com.rentflow.payment.query.ListDepositsQuery;
import com.rentflow.payment.query.ListInvoicesQuery;
import com.rentflow.payment.query.ListRefundsQuery;
import com.rentflow.shared.AuditEntry;
import com.rentflow.shared.DomainEvent;
import com.rentflow.shared.DomainException;
import com.rentflow.shared.ResourceNotFoundException;
import com.rentflow.shared.id.InvoiceId;
import com.rentflow.shared.money.Money;
import com.rentflow.shared.port.out.AuditLogPort;
import com.rentflow.shared.port.out.DomainEventPublisher;
import com.rentflow.shared.port.out.PdfGeneratorPort;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

@Service
@Transactional
public class PaymentApplicationService implements GetInvoiceUseCase, ListInvoicesUseCase, SendInvoiceUseCase,
        VoidInvoiceUseCase, RecordPaymentUseCase, GenerateInvoicePdfUseCase, GetDepositUseCase,
        ListDepositsUseCase, ReleaseDepositUseCase, ForfeitDepositUseCase, RequestRefundUseCase,
        ApproveRefundUseCase, ProcessRefundUseCase, ListRefundsUseCase, CreateInvoiceForContractUseCase {

    private final InvoiceRepository invoiceRepository;
    private final DepositRepository depositRepository;
    private final RefundRepository refundRepository;
    private final DomainEventPublisher eventPublisher;
    private final AuditLogPort auditLog;
    private final PdfGeneratorPort pdfGenerator;

    public PaymentApplicationService(InvoiceRepository invoiceRepository, DepositRepository depositRepository,
                                     RefundRepository refundRepository, DomainEventPublisher eventPublisher,
                                     AuditLogPort auditLog, PdfGeneratorPort pdfGenerator) {
        this.invoiceRepository = invoiceRepository;
        this.depositRepository = depositRepository;
        this.refundRepository = refundRepository;
        this.eventPublisher = eventPublisher;
        this.auditLog = auditLog;
        this.pdfGenerator = pdfGenerator;
    }

    @Override
    public InvoiceId createForContract(CreateInvoiceForContractCommand command) {
        Currency currency = command.currency();
        List<LineItem> lineItems = new ArrayList<>();
        lineItems.add(new LineItem("Rental - " + command.rentalDays() + " day(s)",
                InvoiceLineItemType.RENTAL_BASE, command.rentalBaseAmount(), 1));

        if (isPositive(command.discountAmount())) {
            lineItems.add(new LineItem("Discount", InvoiceLineItemType.DISCOUNT,
                    command.discountAmount().multiply(new BigDecimal("-1")), 1));
        }
        if (isPositive(command.lateFee())) {
            lineItems.add(new LineItem("Late return fee", InvoiceLineItemType.LATE_FEE, command.lateFee(), 1));
        }
        if (isPositive(command.fuelSurcharge())) {
            lineItems.add(new LineItem("Fuel surcharge", InvoiceLineItemType.FUEL_SURCHARGE,
                    command.fuelSurcharge(), 1));
        }
        if (isPositive(command.taxAmount())) {
            lineItems.add(new LineItem("VAT", InvoiceLineItemType.TAX, command.taxAmount(), 1));
        }

        LocalDate issueDate = LocalDate.now();
        Invoice invoice = Invoice.create(command.contractId(), command.customerId(), lineItems, issueDate.plusDays(7));
        invoice.send();
        invoiceRepository.save(invoice);

        if (command.depositAmount().amount().compareTo(BigDecimal.ZERO) > 0) {
            Deposit deposit = Deposit.hold(command.contractId(), command.customerId(), invoice.getId(),
                    command.depositAmount());
            depositRepository.save(deposit);
            publishEvents(deposit.pullDomainEvents());
        }

        publishEvents(invoice.pullDomainEvents());
        auditLog.log(AuditEntry.of("INVOICE_CREATED_FOR_CONTRACT", invoice.getId(), null));
        return invoice.getId();
    }

    @Override
    public void send(SendInvoiceCommand command) {
        Invoice invoice = loadInvoice(command.invoiceId());
        invoice.send();
        invoiceRepository.save(invoice);
        publishEvents(invoice.pullDomainEvents());
        auditLog.log(AuditEntry.of("INVOICE_SENT", invoice.getId(), command.sentBy()));
    }

    @Override
    public void voidInvoice(VoidInvoiceCommand command) {
        Invoice invoice = loadInvoice(command.invoiceId());
        invoice.voidInvoice();
        invoiceRepository.save(invoice);
        auditLog.log(AuditEntry.of("INVOICE_VOIDED", invoice.getId(), command.voidedBy()));
    }

    @Override
    public void record(RecordPaymentCommand command) {
        Invoice invoice = loadInvoice(command.invoiceId());
        if (command.amount().isGreaterThan(invoice.outstandingAmount())) {
            throw new DomainException("Payment amount " + formatMoney(command.amount())
                    + " exceeds outstanding balance " + formatMoney(invoice.outstandingAmount()));
        }
        invoice.recordPayment(command.amount(), command.method(), command.gatewayReference());
        invoiceRepository.save(invoice);
        publishEvents(invoice.pullDomainEvents());
        auditLog.log(AuditEntry.of("PAYMENT_RECORDED", invoice.getId(), command.recordedBy()));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generate(InvoiceId id) {
        return pdfGenerator.generateInvoice(toDetail(loadInvoice(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceDetail get(InvoiceId id) {
        return toDetail(loadInvoice(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceSummary> list(ListInvoicesQuery query) {
        return invoiceRepository.findAll(query);
    }

    @Override
    @Transactional(readOnly = true)
    public DepositDetail get(DepositId id) {
        return toDetail(loadDeposit(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DepositSummary> list(ListDepositsQuery query) {
        return depositRepository.findAll(query);
    }

    @Override
    public void release(ReleaseDepositCommand command) {
        Deposit deposit = loadDeposit(command.depositId());
        deposit.release(command.reason());
        depositRepository.save(deposit);
        publishEvents(deposit.pullDomainEvents());
        auditLog.log(AuditEntry.of("DEPOSIT_RELEASED", deposit.getId(), command.releasedBy()));
    }

    @Override
    public void forfeit(ForfeitDepositCommand command) {
        Deposit deposit = loadDeposit(command.depositId());
        deposit.forfeit(command.reason());
        depositRepository.save(deposit);
        publishEvents(deposit.pullDomainEvents());
        auditLog.log(AuditEntry.of("DEPOSIT_FORFEITED", deposit.getId(), command.forfeitedBy()));
    }

    @Override
    public RefundId request(RequestRefundCommand command) {
        Invoice invoice = loadInvoice(command.invoiceId());
        if (invoice.getStatus() != InvoiceStatus.PAID && invoice.getStatus() != InvoiceStatus.PARTIALLY_PAID) {
            throw new DomainException("Refunds can only be requested on paid invoices");
        }
        if (command.amount().isGreaterThan(invoice.getPaidAmount())) {
            throw new DomainException("Refund amount exceeds paid amount");
        }
        Refund refund = Refund.request(invoice.getId(), invoice.getCustomerId(), command.amount(), command.reason(),
                command.method(), command.notes());
        refundRepository.save(refund);
        publishEvents(refund.pullDomainEvents());
        auditLog.log(AuditEntry.of("REFUND_REQUESTED", refund.getId(), command.requestedBy()));
        return refund.getId();
    }

    @Override
    public void approve(ApproveRefundCommand command) {
        Refund refund = loadRefund(command.refundId());
        refund.approve(command.approvedBy());
        refundRepository.save(refund);
        publishEvents(refund.pullDomainEvents());
        auditLog.log(AuditEntry.of("REFUND_APPROVED", refund.getId(), command.approvedBy()));
    }

    @Override
    public void process(ProcessRefundCommand command) {
        Refund refund = loadRefund(command.refundId());
        refund.process(command.processedBy());
        refundRepository.save(refund);
        publishEvents(refund.pullDomainEvents());
        auditLog.log(AuditEntry.of("REFUND_PROCESSED", refund.getId(), command.processedBy()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RefundSummary> list(ListRefundsQuery query) {
        return refundRepository.findAll(query);
    }

    private Invoice loadInvoice(InvoiceId id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id.value()));
    }

    private Deposit loadDeposit(DepositId id) {
        return depositRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deposit not found: " + id.value()));
    }

    private Refund loadRefund(RefundId id) {
        return refundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found: " + id.value()));
    }

    private void publishEvents(List<DomainEvent> events) {
        events.forEach(eventPublisher::publish);
    }

    private static InvoiceDetail toDetail(Invoice invoice) {
        return new InvoiceDetail(invoice.getId(), invoice.getInvoiceNumber(), invoice.getContractId(),
                invoice.getCustomerId(), invoice.getStatus(), invoice.getLineItems(), invoice.totalAmount(),
                invoice.getPaidAmount(), invoice.outstandingAmount(), invoice.getIssueDate(), invoice.getDueDate(),
                invoice.getPayments());
    }

    private static DepositDetail toDetail(Deposit deposit) {
        return new DepositDetail(deposit.getId(), deposit.getContractId(), deposit.getCustomerId(),
                deposit.getInvoiceId(), deposit.getAmount(), deposit.getStatus(), deposit.getReleaseReason(),
                deposit.getForfeitReason(), deposit.getHeldAt(), deposit.getSettledAt());
    }

    private static boolean isPositive(Money money) {
        return money.amount().compareTo(BigDecimal.ZERO) > 0;
    }

    private static String formatMoney(Money money) {
        return money.currency().getSymbol() + money.amount().setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
