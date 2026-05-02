package com.rentflow.payment.adapter.out.persistence;

import com.rentflow.payment.Refund;
import com.rentflow.payment.RefundId;
import com.rentflow.payment.model.RefundSummary;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.InvoiceId;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.money.Money;
import org.springframework.stereotype.Component;

import java.util.Currency;

@Component
public class RefundJpaMapper {

    public RefundJpaEntity toJpa(Refund domain) {
        var entity = new RefundJpaEntity();
        entity.id = domain.getId().value();
        entity.invoiceId = domain.getInvoiceId().value();
        entity.customerId = domain.getCustomerId().value();
        entity.amount = domain.getAmount().amount();
        entity.currency = domain.getAmount().currency().getCurrencyCode();
        entity.reason = domain.getReason();
        entity.method = domain.getMethod();
        entity.notes = domain.getNotes();
        entity.status = domain.getStatus();
        entity.approvedBy = domain.getApprovedBy() == null ? null : domain.getApprovedBy().value();
        entity.processedBy = domain.getProcessedBy() == null ? null : domain.getProcessedBy().value();
        entity.requestedAt = domain.getRequestedAt();
        entity.processedAt = domain.getProcessedAt();
        return entity;
    }

    public Refund toDomain(RefundJpaEntity entity) {
        Currency currency = Currency.getInstance(entity.currency);
        return Refund.reconstitute(RefundId.of(entity.id), InvoiceId.of(entity.invoiceId),
                CustomerId.of(entity.customerId), new Money(entity.amount, currency), entity.reason, entity.method,
                entity.notes, entity.status, entity.approvedBy == null ? null : StaffId.of(entity.approvedBy),
                entity.processedBy == null ? null : StaffId.of(entity.processedBy), entity.requestedAt,
                entity.processedAt);
    }

    public RefundSummary toSummary(RefundJpaEntity entity) {
        Currency currency = Currency.getInstance(entity.currency);
        return new RefundSummary(RefundId.of(entity.id), InvoiceId.of(entity.invoiceId),
                CustomerId.of(entity.customerId), new Money(entity.amount, currency), entity.reason, entity.status,
                entity.requestedAt);
    }
}
