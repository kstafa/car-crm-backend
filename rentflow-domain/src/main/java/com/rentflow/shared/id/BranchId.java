package com.rentflow.shared.id;

import java.util.Objects;
import java.util.UUID;

public record BranchId(UUID value) {
    public BranchId {
        Objects.requireNonNull(value);
    }

    public static BranchId generate() {
        return new BranchId(UUID.randomUUID());
    }

    public static BranchId of(UUID value) {
        return new BranchId(value);
    }

    public static BranchId of(String value) {
        return new BranchId(UUID.fromString(value));
    }
}
