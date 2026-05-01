package com.rentflow.shared.id;

import java.util.Objects;
import java.util.UUID;

public record InvoiceId(UUID value) {
    public InvoiceId {
        Objects.requireNonNull(value);
    }

    public static InvoiceId generate() {
        return new InvoiceId(UUID.randomUUID());
    }

    public static InvoiceId of(UUID value) {
        return new InvoiceId(value);
    }

    public static InvoiceId of(String value) {
        return new InvoiceId(UUID.fromString(value));
    }
}
