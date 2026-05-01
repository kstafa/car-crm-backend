package com.rentflow.reservation.port.in;

import com.rentflow.reservation.command.ConfirmReservationCommand;

public interface ConfirmReservationUseCase {
    void confirm(ConfirmReservationCommand cmd);
}
