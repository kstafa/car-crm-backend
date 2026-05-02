package com.rentflow.reservation.adapter.out.persistence;

import com.rentflow.AbstractJpaAdapterTest;
import com.rentflow.reservation.DateRange;
import com.rentflow.reservation.Reservation;
import com.rentflow.reservation.ReservationStatus;
import com.rentflow.reservation.model.ReservationSummary;
import com.rentflow.reservation.query.ListReservationsQuery;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;
import com.rentflow.shared.money.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import({JpaReservationRepository.class, ReservationJpaMapper.class})
class JpaReservationRepositoryTest extends AbstractJpaAdapterTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final ZonedDateTime PICKUP = ZonedDateTime.of(2026, 7, 1, 9, 0, 0, 0, ZoneId.of("UTC"));

    @Autowired
    private JpaReservationRepository repository;
    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void save_thenFindById_domainAggregateIsReconstituted() {
        CustomerId customerId = insertCustomer();
        VehicleId vehicleId = insertVehicle("AB-123-CD");
        Reservation reservation = reservation(customerId, vehicleId, PICKUP, PICKUP.plusDays(3));

        repository.save(reservation);
        entityManager.flush();
        entityManager.clear();

        Optional<Reservation> found = repository.findById(reservation.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getReservationNumber()).isEqualTo(reservation.getReservationNumber());
        assertThat(found.get().getRentalPeriod()).isEqualTo(reservation.getRentalPeriod());
    }

    @Test
    void findConflicting_overlappingConfirmed_returnsConflict() {
        VehicleId vehicleId = insertVehicle("AB-123-CD");
        Reservation reservation = confirmedReservation(insertCustomer(), vehicleId, PICKUP, PICKUP.plusDays(3));
        repository.save(reservation);

        List<Reservation> result = repository.findConflicting(vehicleId,
                new DateRange(PICKUP.plusDays(1), PICKUP.plusDays(4)));

        assertThat(result).extracting(Reservation::getId).containsExactly(reservation.getId());
    }

    @Test
    void findConflicting_cancelledReservation_excluded() {
        VehicleId vehicleId = insertVehicle("AB-123-CD");
        Reservation reservation = reservation(insertCustomer(), vehicleId, PICKUP, PICKUP.plusDays(3));
        reservation.cancel("customer request");
        repository.save(reservation);

        List<Reservation> result = repository.findConflicting(vehicleId,
                new DateRange(PICKUP.plusDays(1), PICKUP.plusDays(4)));

        assertThat(result).isEmpty();
    }

    @Test
    void findConflicting_adjacentReservation_excluded() {
        VehicleId vehicleId = insertVehicle("AB-123-CD");
        Reservation reservation = confirmedReservation(insertCustomer(), vehicleId, PICKUP, PICKUP.plusDays(3));
        repository.save(reservation);

        List<Reservation> result = repository.findConflicting(vehicleId,
                new DateRange(PICKUP.plusDays(3), PICKUP.plusDays(4)));

        assertThat(result).isEmpty();
    }

    @Test
    void findTodayPickups_returnsOnlyTodayConfirmedReservations() {
        ZonedDateTime today = LocalDate.now().atTime(10, 0).atZone(ZoneId.systemDefault());
        Reservation todayConfirmed = confirmedReservation(insertCustomer(), insertVehicle("AA-111-AA"), today,
                today.plusDays(2));
        Reservation todayDraft = reservation(insertCustomer(), insertVehicle("BB-111-BB"), today.plusHours(1),
                today.plusDays(3));
        Reservation tomorrowConfirmed = confirmedReservation(insertCustomer(), insertVehicle("CC-111-CC"),
                today.plusDays(1), today.plusDays(4));
        repository.save(todayConfirmed);
        repository.save(todayDraft);
        repository.save(tomorrowConfirmed);

        List<ReservationSummary> result = repository.findTodayPickups();

        assertThat(result).extracting(ReservationSummary::id).containsExactly(todayConfirmed.getId());
    }

    @Test
    void findTodayReturns_returnsOnlyTodayActiveReservations() {
        ZonedDateTime today = LocalDate.now().atTime(10, 0).atZone(ZoneId.systemDefault());
        Reservation active = confirmedReservation(insertCustomer(), insertVehicle("AA-111-AA"), today.minusDays(2),
                today);
        active.activate();
        Reservation confirmed = confirmedReservation(insertCustomer(), insertVehicle("BB-111-BB"),
                today.minusDays(2), today.plusHours(1));
        repository.save(active);
        repository.save(confirmed);

        List<ReservationSummary> result = repository.findTodayReturns();

        assertThat(result).extracting(ReservationSummary::id).containsExactly(active.getId());
    }

    @Test
    void findOverdue_returnsActiveReservationsPastReturnDate() {
        ZonedDateTime now = ZonedDateTime.now();
        Reservation overdue = confirmedReservation(insertCustomer(), insertVehicle("AA-111-AA"), now.minusDays(3),
                now.minusDays(1));
        overdue.activate();
        Reservation future = confirmedReservation(insertCustomer(), insertVehicle("BB-111-BB"), now.minusDays(1),
                now.plusDays(1));
        future.activate();
        repository.save(overdue);
        repository.save(future);

        List<ReservationSummary> result = repository.findOverdue();

        assertThat(result).extracting(ReservationSummary::id).containsExactly(overdue.getId());
    }

    @Test
    void findAll_filterByStatus_returnsMatchingPage() {
        Reservation confirmed = confirmedReservation(insertCustomer(), insertVehicle("AA-111-AA"), PICKUP,
                PICKUP.plusDays(3));
        Reservation cancelled = reservation(insertCustomer(), insertVehicle("BB-111-BB"), PICKUP.plusDays(5),
                PICKUP.plusDays(7));
        cancelled.cancel("customer request");
        repository.save(confirmed);
        repository.save(cancelled);

        Page<ReservationSummary> result = repository.findAll(new ListReservationsQuery(ReservationStatus.CANCELLED,
                null, null, 0, 20));

        assertThat(result.getContent()).extracting(ReservationSummary::id).containsExactly(cancelled.getId());
    }

    private Reservation confirmedReservation(CustomerId customerId, VehicleId vehicleId, ZonedDateTime pickup,
                                             ZonedDateTime returns) {
        Reservation reservation = reservation(customerId, vehicleId, pickup, returns);
        reservation.confirm();
        return reservation;
    }

    private Reservation reservation(CustomerId customerId, VehicleId vehicleId, ZonedDateTime pickup,
                                    ZonedDateTime returns) {
        return Reservation.create(customerId, vehicleId, new DateRange(pickup, returns), money("300.00"),
                Money.zero(EUR), Money.zero(EUR));
    }

    private CustomerId insertCustomer() {
        CustomerId id = CustomerId.of(UUID.randomUUID());
        jdbc.update("""
                INSERT INTO customers (id, first_name, last_name, email, status)
                VALUES (?, 'Ada', 'Lovelace', ?, 'ACTIVE')
                """, id.value(), id.value() + "@example.com");
        return id;
    }

    private VehicleId insertVehicle(String licensePlate) {
        VehicleCategoryId categoryId = VehicleCategoryId.of(UUID.randomUUID());
        jdbc.update("""
                INSERT INTO vehicle_categories
                    (id, name, description, base_daily_rate, deposit_amount, currency, tax_rate)
                VALUES (?, ?, 'Test category', 49.99, 300.00, 'EUR', 0.20)
                """, categoryId.value(), "Category-" + categoryId.value());
        VehicleId vehicleId = VehicleId.of(UUID.randomUUID());
        jdbc.update("""
                INSERT INTO vehicles
                    (id, license_plate, brand, model, year, current_mileage, status, category_id, active)
                VALUES (?, ?, 'Toyota', 'Yaris', 2024, 1000, 'AVAILABLE', ?, TRUE)
                """, vehicleId.value(), licensePlate, categoryId.value());
        return vehicleId;
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), EUR);
    }
}
