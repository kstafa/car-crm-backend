package com.rentflow.shared.id;

import java.util.Objects;
import java.util.UUID;

public record VehicleId(UUID value) {
    public VehicleId {
        Objects.requireNonNull(value);
    }

    public static VehicleId generate() {
        return new VehicleId(UUID.randomUUID());
    }

    public static VehicleId of(UUID value) {
        return new VehicleId(value);
    }

    public static VehicleId of(String value) {
        return new VehicleId(UUID.fromString(value));
    }
}
