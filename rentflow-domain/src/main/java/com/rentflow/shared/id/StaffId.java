package com.rentflow.shared.id;

import java.util.Objects;
import java.util.UUID;

public record StaffId(UUID value) {
    public StaffId {
        Objects.requireNonNull(value);
    }

    public static StaffId generate() {
        return new StaffId(UUID.randomUUID());
    }

    public static StaffId of(UUID value) {
        return new StaffId(value);
    }

    public static StaffId of(String value) {
        return new StaffId(UUID.fromString(value));
    }
}
