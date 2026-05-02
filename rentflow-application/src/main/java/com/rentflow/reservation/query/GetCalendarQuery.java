package com.rentflow.reservation.query;

import com.rentflow.shared.id.VehicleCategoryId;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public record GetCalendarQuery(
        LocalDate from,
        LocalDate to,
        VehicleCategoryId categoryId
) {
    public GetCalendarQuery {
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(to, "to must not be null");
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("to must not be before from");
        }
        if (ChronoUnit.DAYS.between(from, to) > 90) {
            throw new IllegalArgumentException("Calendar range must not exceed 90 days");
        }
    }
}
