package com.rentflow.reservation.port.in;

import com.rentflow.reservation.model.ReservationSummary;
import com.rentflow.reservation.query.ListReservationsQuery;
import org.springframework.data.domain.Page;

public interface ListReservationsUseCase {
    Page<ReservationSummary> list(ListReservationsQuery q);
}
