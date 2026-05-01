package com.rentflow.shared.id;

import java.util.Objects;
import java.util.UUID;

public record VehicleCategoryId(UUID value) {
    public VehicleCategoryId {
        Objects.requireNonNull(value);
    }

    public static VehicleCategoryId generate() {
        return new VehicleCategoryId(UUID.randomUUID());
    }

    public static VehicleCategoryId of(UUID value) {
        return new VehicleCategoryId(value);
    }

    public static VehicleCategoryId of(String value) {
        return new VehicleCategoryId(UUID.fromString(value));
    }
}
