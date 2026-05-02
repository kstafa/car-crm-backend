package com.rentflow.payment;

import java.util.Objects;
import java.util.UUID;

public record RefundId(UUID value) {
    public RefundId {
        Objects.requireNonNull(value);
    }

    public static RefundId generate() {
        return new RefundId(UUID.randomUUID());
    }

    public static RefundId of(UUID value) {
        return new RefundId(value);
    }

    public static RefundId of(String value) {
        return new RefundId(UUID.fromString(value));
    }
}
