package com.rentflow.shared.adapter.out;

import com.rentflow.reservation.DateRange;
import com.rentflow.reservation.Reservation;
import com.rentflow.reservation.model.ReservationSummary;
import com.rentflow.reservation.port.out.ReservationRepository;
import com.rentflow.reservation.query.ListReservationsQuery;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.VehicleId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

public class NoOpReservationRepository implements ReservationRepository {
    @Override
    public void save(Reservation reservation) {
    }

    @Override
    public Optional<Reservation> findById(ReservationId id) {
        return Optional.empty();
    }

    @Override
    public List<Reservation> findConflicting(VehicleId vehicleId, DateRange period) {
        return List.of();
    }

    @Override
    public Page<ReservationSummary> findAll(ListReservationsQuery query) {
        int page = query == null ? 0 : Math.max(query.page(), 0);
        int size = query == null ? 20 : Math.max(query.size(), 1);
        return Page.empty(PageRequest.of(page, size));
    }

    @Override
    public List<ReservationSummary> findTodayPickups() {
        return List.of();
    }

    @Override
    public List<ReservationSummary> findTodayReturns() {
        return List.of();
    }

    @Override
    public List<ReservationSummary> findOverdue() {
        return List.of();
    }
}
