package com.rentflow.payment;

import com.rentflow.shared.DomainEvent;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.money.Money;

import java.time.Instant;

public record DepositHeldEvent(DepositId depositId, ContractId contractId, Money amount, Instant occurredAt)
        implements DomainEvent {
    public DepositHeldEvent(DepositId depositId, ContractId contractId, Money amount) {
        this(depositId, contractId, amount, Instant.now());
    }
}
