package com.rentflow.contract;

import com.rentflow.shared.DomainEvent;
import com.rentflow.shared.id.ContractId;

import java.time.Instant;
import java.time.ZonedDateTime;

public record ContractExtendedEvent(
        ContractId contractId,
        ZonedDateTime newScheduledReturn,
        Instant occurredAt
) implements DomainEvent {
    public ContractExtendedEvent(ContractId contractId, ZonedDateTime newScheduledReturn) {
        this(contractId, newScheduledReturn, Instant.now());
    }
}
