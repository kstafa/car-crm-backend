package com.rentflow.payment.adapter.in.rest;

import com.rentflow.payment.InvoiceStatus;
import com.rentflow.payment.LineItem;
import com.rentflow.payment.Payment;
import com.rentflow.payment.PaymentMethod;
import com.rentflow.payment.RefundReason;
import com.rentflow.payment.command.ApproveRefundCommand;
import com.rentflow.payment.command.ProcessRefundCommand;
import com.rentflow.payment.command.RecordPaymentCommand;
import com.rentflow.payment.command.RequestRefundCommand;
import com.rentflow.payment.command.SendInvoiceCommand;
import com.rentflow.payment.command.VoidInvoiceCommand;
import com.rentflow.payment.model.InvoiceDetail;
import com.rentflow.payment.model.InvoiceSummary;
import com.rentflow.payment.model.RefundSummary;
import com.rentflow.payment.query.ListInvoicesQuery;
import com.rentflow.payment.query.ListRefundsQuery;
import com.rentflow.shared.adapter.in.rest.PageMeta;
import com.rentflow.shared.adapter.in.rest.PageResponse;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.InvoiceId;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.money.Money;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

@Component
public class InvoiceMapper {

    private static final Currency EUR = Currency.getInstance("EUR");

    public ListInvoicesQuery toQuery(InvoiceStatus status, UUID customerId, UUID contractId, LocalDate from,
                                     LocalDate to, int page, int size) {
        return new ListInvoicesQuery(status, customerId == null ? null : CustomerId.of(customerId),
                contractId == null ? null : ContractId.of(contractId), from, to, page, size);
    }

    public ListRefundsQuery toRefundQuery(int page, int size) {
        return new ListRefundsQuery(null, null, page, size);
    }

    public SendInvoiceCommand toSendCommand(InvoiceId id, StaffId staffId) {
        return new SendInvoiceCommand(id, staffId);
    }

    public VoidInvoiceCommand toVoidCommand(InvoiceId id, StaffId staffId) {
        return new VoidInvoiceCommand(id, staffId);
    }

    public RecordPaymentCommand toCommand(InvoiceId id, RecordPaymentRequest request, StaffId staffId) {
        return new RecordPaymentCommand(id, new Money(request.amount(), EUR), PaymentMethod.valueOf(request.method()),
                request.gatewayReference(), staffId);
    }

    public RequestRefundCommand toCommand(InvoiceId id, RequestRefundRequest request, StaffId staffId) {
        return new RequestRefundCommand(id, new Money(request.amount(), EUR), RefundReason.valueOf(request.reason()),
                PaymentMethod.valueOf(request.method()), request.notes(), staffId);
    }

    public ApproveRefundCommand toApproveCommand(com.rentflow.payment.RefundId id, StaffId staffId) {
        return new ApproveRefundCommand(id, staffId);
    }

    public ProcessRefundCommand toProcessCommand(com.rentflow.payment.RefundId id, StaffId staffId) {
        return new ProcessRefundCommand(id, staffId);
    }

    public PageResponse<InvoiceSummaryResponse> toPageResponse(Page<InvoiceSummary> page) {
        return new PageResponse<>(page.getContent().stream().map(this::toSummaryResponse).toList(),
                new PageMeta(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()));
    }

    public PageResponse<RefundSummaryResponse> toRefundPageResponse(Page<RefundSummary> page) {
        return new PageResponse<>(page.getContent().stream().map(this::toResponse).toList(),
                new PageMeta(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()));
    }

    public InvoiceSummaryResponse toSummaryResponse(InvoiceSummary summary) {
        return new InvoiceSummaryResponse(summary.id().value(), summary.invoiceNumber(), summary.contractId().value(),
                summary.customerId().value(), summary.status().name(), summary.totalAmount().amount(),
                summary.paidAmount().amount(), summary.outstandingAmount().amount(),
                summary.totalAmount().currency().getCurrencyCode(), summary.issueDate(), summary.dueDate());
    }

    public InvoiceDetailResponse toDetailResponse(InvoiceDetail detail) {
        return new InvoiceDetailResponse(detail.id().value(), detail.invoiceNumber(), detail.contractId().value(),
                detail.customerId().value(), detail.status().name(),
                detail.lineItems().stream().map(this::toResponse).toList(), detail.totalAmount().amount(),
                detail.paidAmount().amount(), detail.outstandingAmount().amount(),
                detail.totalAmount().currency().getCurrencyCode(), detail.issueDate(), detail.dueDate(),
                detail.payments().stream().map(this::toResponse).toList());
    }

    public RefundSummaryResponse toResponse(RefundSummary summary) {
        return new RefundSummaryResponse(summary.id().value(), summary.invoiceId().value(),
                summary.customerId().value(), summary.amount().amount(), summary.amount().currency().getCurrencyCode(),
                summary.reason().name(), summary.status().name(), summary.requestedAt());
    }

    private LineItemResponse toResponse(LineItem item) {
        return new LineItemResponse(item.description(), item.type().name(), item.unitPrice().amount(),
                item.quantity(), item.total().amount(), item.unitPrice().currency().getCurrencyCode());
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(payment.id().value(), payment.amount().amount(),
                payment.amount().currency().getCurrencyCode(), payment.method().name(),
                payment.gatewayReference(), payment.paidAt());
    }
}
