package com.rentflow.payment.model;

import com.rentflow.payment.RefundId;
import com.rentflow.payment.RefundReason;
import com.rentflow.payment.RefundStatus;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.InvoiceId;
import com.rentflow.shared.money.Money;

import java.time.Instant;

public record RefundSummary(
        RefundId id,
        InvoiceId invoiceId,
        CustomerId customerId,
        Money amount,
        RefundReason reason,
        RefundStatus status,
        Instant requestedAt
) {
}
