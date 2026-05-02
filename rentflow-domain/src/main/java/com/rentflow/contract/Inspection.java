package com.rentflow.contract;

import com.rentflow.shared.FuelLevel;
import com.rentflow.shared.id.StaffId;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record Inspection(
        InspectionType type,
        InspectionChecklist checklist,
        FuelLevel fuelLevel,
        int mileage,
        List<String> photoKeys,
        Instant performedAt,
        StaffId performedBy
) {
    public enum InspectionType { PRE, POST }

    public Inspection {
        Objects.requireNonNull(type);
        Objects.requireNonNull(checklist);
        Objects.requireNonNull(fuelLevel);
        Objects.requireNonNull(performedAt);
        Objects.requireNonNull(performedBy);
        if (mileage < 0) {
            throw new IllegalArgumentException("mileage must be >= 0");
        }
        photoKeys = photoKeys == null ? List.of() : List.copyOf(photoKeys);
    }
}
