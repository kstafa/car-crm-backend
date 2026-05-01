package com.rentflow.reservation.port.in;

import com.rentflow.reservation.model.ReservationDetail;
import com.rentflow.shared.id.ReservationId;

public interface GetReservationUseCase {
    ReservationDetail get(ReservationId id);
}
