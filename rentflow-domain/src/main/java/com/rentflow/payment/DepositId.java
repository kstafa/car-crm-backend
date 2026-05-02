package com.rentflow.payment;

import java.util.Objects;
import java.util.UUID;

public record DepositId(UUID value) {
    public DepositId {
        Objects.requireNonNull(value);
    }

    public static DepositId generate() {
        return new DepositId(UUID.randomUUID());
    }

    public static DepositId of(UUID value) {
        return new DepositId(value);
    }

    public static DepositId of(String value) {
        return new DepositId(UUID.fromString(value));
    }
}
