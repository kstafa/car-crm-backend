package com.rentflow.payment.model;

import com.rentflow.payment.DepositId;
import com.rentflow.payment.DepositStatus;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.money.Money;

import java.time.Instant;

public record DepositSummary(
        DepositId id,
        ContractId contractId,
        CustomerId customerId,
        Money amount,
        DepositStatus status,
        Instant heldAt,
        Instant settledAt
) {
}
