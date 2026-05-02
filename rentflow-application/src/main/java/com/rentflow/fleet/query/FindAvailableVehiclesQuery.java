package com.rentflow.fleet.query;

import com.rentflow.reservation.DateRange;
import com.rentflow.shared.id.VehicleCategoryId;

import java.time.ZonedDateTime;
import java.util.Objects;

public record FindAvailableVehiclesQuery(
        VehicleCategoryId categoryId,
        ZonedDateTime pickupDatetime,
        ZonedDateTime returnDatetime
) {
    public FindAvailableVehiclesQuery {
        Objects.requireNonNull(categoryId);
        Objects.requireNonNull(pickupDatetime);
        Objects.requireNonNull(returnDatetime);
        if (!returnDatetime.isAfter(pickupDatetime)) {
            throw new IllegalArgumentException("returnDatetime must be after pickupDatetime");
        }
    }

    public DateRange toDateRange() {
        return new DateRange(pickupDatetime, returnDatetime);
    }
}
