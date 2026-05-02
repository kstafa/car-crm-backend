package com.rentflow.payment;

import com.rentflow.shared.DomainEvent;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.money.Money;

import java.time.Instant;

public record DepositForfeitedEvent(
        DepositId depositId,
        ContractId contractId,
        Money amount,
        String reason,
        Instant occurredAt
) implements DomainEvent {
    public DepositForfeitedEvent(DepositId depositId, ContractId contractId, Money amount, String reason) {
        this(depositId, contractId, amount, reason, Instant.now());
    }
}
