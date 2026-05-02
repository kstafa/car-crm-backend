package com.rentflow.payment.model;

import com.rentflow.payment.DepositId;
import com.rentflow.payment.DepositStatus;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.InvoiceId;
import com.rentflow.shared.money.Money;

import java.time.Instant;

public record DepositDetail(
        DepositId id,
        ContractId contractId,
        CustomerId customerId,
        InvoiceId invoiceId,
        Money amount,
        DepositStatus status,
        String releaseReason,
        String forfeitReason,
        Instant heldAt,
        Instant settledAt
) {
}
