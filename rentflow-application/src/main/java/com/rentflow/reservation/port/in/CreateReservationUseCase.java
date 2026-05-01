package com.rentflow.reservation.port.in;

import com.rentflow.reservation.command.CreateReservationCommand;
import com.rentflow.shared.id.ReservationId;

public interface CreateReservationUseCase {
    ReservationId create(CreateReservationCommand cmd);
}
