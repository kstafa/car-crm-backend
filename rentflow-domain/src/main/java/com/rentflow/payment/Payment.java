package com.rentflow.payment;

import com.rentflow.shared.money.Money;

import java.time.Instant;
import java.util.Objects;

public record Payment(
        PaymentId id,
        Money amount,
        PaymentMethod method,
        String gatewayReference,
        Instant paidAt
) {
    public Payment {
        Objects.requireNonNull(id);
        Objects.requireNonNull(amount);
        Objects.requireNonNull(method);
        Objects.requireNonNull(paidAt);
    }
}
