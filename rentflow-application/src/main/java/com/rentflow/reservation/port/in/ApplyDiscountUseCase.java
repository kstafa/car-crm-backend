package com.rentflow.reservation.port.in;

import com.rentflow.reservation.command.ApplyDiscountCommand;

public interface ApplyDiscountUseCase {
    void apply(ApplyDiscountCommand command);
}
