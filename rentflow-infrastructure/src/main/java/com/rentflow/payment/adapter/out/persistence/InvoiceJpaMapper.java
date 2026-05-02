package com.rentflow.payment.adapter.out.persistence;

import com.rentflow.payment.Invoice;
import com.rentflow.payment.LineItem;
import com.rentflow.payment.Payment;
import com.rentflow.payment.PaymentId;
import com.rentflow.payment.model.InvoiceDetail;
import com.rentflow.payment.model.InvoiceSummary;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.InvoiceId;
import com.rentflow.shared.money.Money;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Currency;
import java.util.UUID;

@Component
public class InvoiceJpaMapper {

    public InvoiceJpaEntity toJpa(Invoice domain) {
        var entity = new InvoiceJpaEntity();
        entity.id = domain.getId().value();
        entity.invoiceNumber = domain.getInvoiceNumber();
        entity.contractId = domain.getContractId().value();
        entity.customerId = domain.getCustomerId().value();
        entity.status = domain.getStatus();
        entity.issueDate = domain.getIssueDate();
        entity.dueDate = domain.getDueDate();
        entity.paidAmount = domain.getPaidAmount().amount();
        entity.currency = domain.getPaidAmount().currency().getCurrencyCode();
        entity.lineItems = new ArrayList<>(domain.getLineItems().stream().map(this::toJpa).toList());
        for (int i = 0; i < entity.lineItems.size(); i++) {
            InvoiceLineItemJpaEntity lineItem = entity.lineItems.get(i);
            lineItem.invoice = entity;
            lineItem.sortOrder = i;
        }
        entity.payments = new ArrayList<>(domain.getPayments().stream().map(this::toJpa).toList());
        entity.payments.forEach(payment -> payment.invoice = entity);
        return entity;
    }

    public Invoice toDomain(InvoiceJpaEntity entity) {
        Currency currency = Currency.getInstance(entity.currency);
        return Invoice.reconstitute(InvoiceId.of(entity.id), entity.invoiceNumber,
                ContractId.of(entity.contractId), CustomerId.of(entity.customerId), entity.status,
                entity.lineItems.stream().map(item -> toDomain(item, currency)).toList(),
                new Money(entity.paidAmount, currency), entity.issueDate, entity.dueDate,
                entity.payments.stream().map(payment -> toDomain(payment, currency)).toList());
    }

    public InvoiceSummary toSummary(InvoiceJpaEntity entity) {
        Currency currency = Currency.getInstance(entity.currency);
        Money total = total(entity, currency);
        Money paid = new Money(entity.paidAmount, currency);
        return new InvoiceSummary(InvoiceId.of(entity.id), entity.invoiceNumber, ContractId.of(entity.contractId),
                CustomerId.of(entity.customerId), entity.status, total, paid, total.subtract(paid),
                entity.issueDate, entity.dueDate);
    }

    public InvoiceDetail toDetail(InvoiceJpaEntity entity) {
        Currency currency = Currency.getInstance(entity.currency);
        Money total = total(entity, currency);
        Money paid = new Money(entity.paidAmount, currency);
        return new InvoiceDetail(InvoiceId.of(entity.id), entity.invoiceNumber, ContractId.of(entity.contractId),
                CustomerId.of(entity.customerId), entity.status,
                entity.lineItems.stream().map(item -> toDomain(item, currency)).toList(), total, paid,
                total.subtract(paid), entity.issueDate, entity.dueDate,
                entity.payments.stream().map(payment -> toDomain(payment, currency)).toList());
    }

    private InvoiceLineItemJpaEntity toJpa(LineItem lineItem) {
        var entity = new InvoiceLineItemJpaEntity();
        entity.id = UUID.randomUUID();
        entity.description = lineItem.description();
        entity.type = lineItem.type();
        entity.unitPrice = lineItem.unitPrice().amount();
        entity.currency = lineItem.unitPrice().currency().getCurrencyCode();
        entity.quantity = lineItem.quantity();
        return entity;
    }

    private InvoicePaymentJpaEntity toJpa(Payment payment) {
        var entity = new InvoicePaymentJpaEntity();
        entity.id = payment.id().value();
        entity.amount = payment.amount().amount();
        entity.currency = payment.amount().currency().getCurrencyCode();
        entity.method = payment.method();
        entity.gatewayReference = payment.gatewayReference();
        entity.paidAt = payment.paidAt();
        return entity;
    }

    private LineItem toDomain(InvoiceLineItemJpaEntity entity, Currency invoiceCurrency) {
        Currency currency = entity.currency == null ? invoiceCurrency : Currency.getInstance(entity.currency);
        return new LineItem(entity.description, entity.type, new Money(entity.unitPrice, currency), entity.quantity);
    }

    private Payment toDomain(InvoicePaymentJpaEntity entity, Currency invoiceCurrency) {
        Currency currency = entity.currency == null ? invoiceCurrency : Currency.getInstance(entity.currency);
        return new Payment(PaymentId.of(entity.id), new Money(entity.amount, currency), entity.method,
                entity.gatewayReference, entity.paidAt);
    }

    private Money total(InvoiceJpaEntity entity, Currency currency) {
        Money total = Money.zero(currency);
        for (InvoiceLineItemJpaEntity item : entity.lineItems) {
            total = total.add(toDomain(item, currency).total());
        }
        return total;
    }
}
