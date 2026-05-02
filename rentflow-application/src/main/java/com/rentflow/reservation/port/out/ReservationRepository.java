package com.rentflow.reservation.port.out;

import com.rentflow.reservation.DateRange;
import com.rentflow.reservation.Reservation;
import com.rentflow.reservation.model.ConflictRow;
import com.rentflow.reservation.model.ReservationCalendarRow;
import com.rentflow.reservation.model.ReservationSummary;
import com.rentflow.reservation.query.ListReservationsQuery;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    void save(Reservation reservation);

    Optional<Reservation> findById(ReservationId id);

    List<Reservation> findConflicting(VehicleId vehicleId, DateRange period);

    Page<ReservationSummary> findAll(ListReservationsQuery query);

    List<ReservationSummary> findTodayPickups();

    List<ReservationSummary> findTodayReturns();

    List<ReservationSummary> findOverdue();

    List<ReservationCalendarRow> findForCalendar(LocalDate from, LocalDate to, VehicleCategoryId categoryId);

    List<ConflictRow> findDraftConflicts();
}
