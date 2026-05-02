package com.rentflow.contract;

import java.util.Objects;
import java.util.UUID;

public record DamageReportId(UUID value) {
    public DamageReportId {
        Objects.requireNonNull(value);
    }

    public static DamageReportId generate() {
        return new DamageReportId(UUID.randomUUID());
    }

    public static DamageReportId of(UUID value) {
        return new DamageReportId(value);
    }

    public static DamageReportId of(String value) {
        return new DamageReportId(UUID.fromString(value));
    }
}
