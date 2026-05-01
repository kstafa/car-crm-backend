package com.rentflow.reservation.query;

import com.rentflow.reservation.ReservationStatus;

import java.time.LocalDate;

public record ListReservationsQuery(ReservationStatus status, LocalDate from, LocalDate to, int page, int size) {
}
