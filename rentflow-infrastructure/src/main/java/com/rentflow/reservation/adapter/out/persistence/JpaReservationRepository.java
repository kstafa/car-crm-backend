package com.rentflow.reservation.adapter.out.persistence;

import com.rentflow.reservation.DateRange;
import com.rentflow.reservation.Reservation;
import com.rentflow.reservation.model.ConflictRow;
import com.rentflow.reservation.model.ReservationCalendarRow;
import com.rentflow.reservation.model.ReservationSummary;
import com.rentflow.reservation.port.out.ReservationRepository;
import com.rentflow.reservation.query.ListReservationsQuery;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Primary
@RequiredArgsConstructor
public class JpaReservationRepository implements ReservationRepository {
    private final SpringDataReservationRepo repo;
    private final ReservationJpaMapper mapper;

    @Override
    public void save(Reservation reservation) {
        ReservationJpaEntity entity = mapper.toJpa(reservation);
        repo.findById(entity.id).ifPresent(existing -> {
            entity.createdAt = existing.createdAt;
            entity.createdBy = existing.createdBy;
            entity.version = existing.version;
        });
        repo.save(entity);
    }

    @Override
    public Optional<Reservation> findById(ReservationId id) {
        return repo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<Reservation> findConflicting(VehicleId vehicleId, DateRange period) {
        return repo.findConflicting(vehicleId.value(), period.start(), period.end())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<ReservationSummary> findAll(ListReservationsQuery q) {
        PageRequest pageable = PageRequest.of(q.page(), q.size());
        ZonedDateTime from = q.from() == null
                ? LocalDate.of(1900, 1, 1).atStartOfDay(ZoneId.systemDefault())
                : q.from().atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime toExclusive = q.to() == null
                ? LocalDate.of(3000, 1, 1).atStartOfDay(ZoneId.systemDefault())
                : q.to().plusDays(1).atStartOfDay(ZoneId.systemDefault());
        return repo.findFiltered(q.status(), from, toExclusive, pageable).map(mapper::toSummary);
    }

    @Override
    public List<ReservationSummary> findTodayPickups() {
        ZonedDateTime start = LocalDate.now().atStartOfDay(ZoneId.systemDefault());
        return repo.findTodayPickups(start, start.plusDays(1)).stream().map(mapper::toSummary).toList();
    }

    @Override
    public List<ReservationSummary> findTodayReturns() {
        ZonedDateTime start = LocalDate.now().atStartOfDay(ZoneId.systemDefault());
        return repo.findTodayReturns(start, start.plusDays(1)).stream().map(mapper::toSummary).toList();
    }

    @Override
    public List<ReservationSummary> findOverdue() {
        return repo.findOverdue(ZonedDateTime.now()).stream().map(mapper::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationCalendarRow> findForCalendar(LocalDate from, LocalDate to, VehicleCategoryId categoryId) {
        ZonedDateTime fromInstant = from.atStartOfDay(ZoneOffset.UTC);
        ZonedDateTime toInstant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC);
        UUID catId = categoryId != null ? categoryId.value() : null;
        return repo.findForCalendar(fromInstant, toInstant, catId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConflictRow> findDraftConflicts() {
        return repo.findDraftConflicts();
    }
}
