package com.rentflow.reservation.port.in;

import com.rentflow.reservation.command.ExtendReservationCommand;

public interface ExtendReservationUseCase {
    void extend(ExtendReservationCommand command);
}
