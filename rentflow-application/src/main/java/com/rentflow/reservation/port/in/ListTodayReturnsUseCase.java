package com.rentflow.reservation.port.in;

import com.rentflow.reservation.model.ReservationSummary;

import java.util.List;

public interface ListTodayReturnsUseCase {
    List<ReservationSummary> listReturns();
}
