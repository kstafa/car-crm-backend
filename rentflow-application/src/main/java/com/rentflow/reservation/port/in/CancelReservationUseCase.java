package com.rentflow.reservation.port.in;

import com.rentflow.reservation.command.CancelReservationCommand;

public interface CancelReservationUseCase {
    void cancel(CancelReservationCommand cmd);
}
