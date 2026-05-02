package com.rentflow.reservation.command;

import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.StaffId;

import java.math.BigDecimal;
import java.util.Objects;

public record ApplyDiscountCommand(
        ReservationId reservationId,
        BigDecimal discountPercent,
        StaffId appliedBy
) {
    public ApplyDiscountCommand {
        Objects.requireNonNull(reservationId);
        Objects.requireNonNull(discountPercent);
        Objects.requireNonNull(appliedBy);
        if (discountPercent.compareTo(BigDecimal.ZERO) < 0
                || discountPercent.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("discountPercent must be between 0.0 and 1.0");
        }
    }
}
