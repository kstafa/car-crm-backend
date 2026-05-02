package com.rentflow.payment.adapter.out.persistence;

import com.rentflow.payment.Deposit;
import com.rentflow.payment.DepositId;
import com.rentflow.payment.model.DepositDetail;
import com.rentflow.payment.model.DepositSummary;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.InvoiceId;
import com.rentflow.shared.money.Money;
import org.springframework.stereotype.Component;

import java.util.Currency;

@Component
public class DepositJpaMapper {

    public DepositJpaEntity toJpa(Deposit domain) {
        var entity = new DepositJpaEntity();
        entity.id = domain.getId().value();
        entity.contractId = domain.getContractId().value();
        entity.customerId = domain.getCustomerId().value();
        entity.invoiceId = domain.getInvoiceId().value();
        entity.amount = domain.getAmount().amount();
        entity.currency = domain.getAmount().currency().getCurrencyCode();
        entity.status = domain.getStatus();
        entity.releaseReason = domain.getReleaseReason();
        entity.forfeitReason = domain.getForfeitReason();
        entity.heldAt = domain.getHeldAt();
        entity.settledAt = domain.getSettledAt();
        return entity;
    }

    public Deposit toDomain(DepositJpaEntity entity) {
        Currency currency = Currency.getInstance(entity.currency);
        return Deposit.reconstitute(DepositId.of(entity.id), ContractId.of(entity.contractId),
                CustomerId.of(entity.customerId), InvoiceId.of(entity.invoiceId), new Money(entity.amount, currency),
                entity.status, entity.releaseReason, entity.forfeitReason, entity.heldAt, entity.settledAt);
    }

    public DepositSummary toSummary(DepositJpaEntity entity) {
        Currency currency = Currency.getInstance(entity.currency);
        return new DepositSummary(DepositId.of(entity.id), ContractId.of(entity.contractId),
                CustomerId.of(entity.customerId), new Money(entity.amount, currency), entity.status, entity.heldAt,
                entity.settledAt);
    }

    public DepositDetail toDetail(DepositJpaEntity entity) {
        Currency currency = Currency.getInstance(entity.currency);
        return new DepositDetail(DepositId.of(entity.id), ContractId.of(entity.contractId),
                CustomerId.of(entity.customerId), InvoiceId.of(entity.invoiceId), new Money(entity.amount, currency),
                entity.status, entity.releaseReason, entity.forfeitReason, entity.heldAt, entity.settledAt);
    }
}
