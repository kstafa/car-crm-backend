package com.rentflow.shared.id;

import java.util.Objects;
import java.util.UUID;

public record ContractId(UUID value) {
    public ContractId {
        Objects.requireNonNull(value);
    }

    public static ContractId generate() {
        return new ContractId(UUID.randomUUID());
    }

    public static ContractId of(UUID value) {
        return new ContractId(value);
    }

    public static ContractId of(String value) {
        return new ContractId(UUID.fromString(value));
    }
}
