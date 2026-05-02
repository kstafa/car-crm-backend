package com.rentflow.fleet.adapter.out.persistence;

import com.rentflow.AbstractJpaAdapterTest;
import com.rentflow.fleet.VehicleStatus;
import com.rentflow.reservation.DateRange;
import com.rentflow.reservation.ReservationStatus;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(JpaVehicleAvailabilityAdapter.class)
class JpaVehicleAvailabilityAdapterTest extends AbstractJpaAdapterTest {

    private static final ZonedDateTime PICKUP = ZonedDateTime.of(2026, 7, 1, 9, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private JpaVehicleAvailabilityAdapter adapter;
    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbc;

    private VehicleCategoryId categoryId;
    private VehicleId vehicleId;

    @BeforeEach
    void setUp() {
        categoryId = VehicleCategoryId.of(UUID.randomUUID());
        vehicleId = VehicleId.of(UUID.randomUUID());
        persistCategory(categoryId, "Economy-" + categoryId.value());
        persistVehicle(vehicleId, categoryId, "AB-123-CD", true);
    }

    @Test
    void isAvailable_noConflictingReservations_returnsTrue() {
        assertThat(adapter.isAvailable(vehicleId, period(PICKUP, PICKUP.plusDays(3)))).isTrue();
    }

    @Test
    void isAvailable_confirmedReservationOverlaps_returnsFalse() {
        insertReservation(vehicleId, PICKUP.plusDays(1), PICKUP.plusDays(4), ReservationStatus.CONFIRMED);

        assertThat(adapter.isAvailable(vehicleId, period(PICKUP, PICKUP.plusDays(3)))).isFalse();
    }

    @Test
    void isAvailable_cancelledReservationOverlaps_returnsTrue() {
        insertReservation(vehicleId, PICKUP.plusDays(1), PICKUP.plusDays(4), ReservationStatus.CANCELLED);

        assertThat(adapter.isAvailable(vehicleId, period(PICKUP, PICKUP.plusDays(3)))).isTrue();
    }

    @Test
    void isAvailable_draftReservationOverlaps_returnsTrue() {
        insertReservation(vehicleId, PICKUP.plusDays(1), PICKUP.plusDays(4), ReservationStatus.DRAFT);

        assertThat(adapter.isAvailable(vehicleId, period(PICKUP, PICKUP.plusDays(3)))).isTrue();
    }

    @Test
    void isAvailable_adjacentReservation_returnsTrue() {
        insertReservation(vehicleId, PICKUP.plusDays(3), PICKUP.plusDays(4), ReservationStatus.CONFIRMED);

        assertThat(adapter.isAvailable(vehicleId, period(PICKUP, PICKUP.plusDays(3)))).isTrue();
    }

    @Test
    void findConflictingVehicleIds_twoVehiclesOneConflicting_returnsOnlyConflicting() {
        VehicleId freeVehicleId = VehicleId.of(UUID.randomUUID());
        persistVehicle(freeVehicleId, categoryId, "CD-123-EF", true);
        insertReservation(vehicleId, PICKUP.plusDays(1), PICKUP.plusDays(4), ReservationStatus.CONFIRMED);

        List<VehicleId> result = adapter.findConflictingVehicleIds(categoryId, period(PICKUP, PICKUP.plusDays(3)));

        assertThat(result).containsExactly(vehicleId);
    }

    @Test
    void findConflictingVehicleIds_allVehiclesFree_returnsEmptyList() {
        VehicleId freeVehicleId = VehicleId.of(UUID.randomUUID());
        persistVehicle(freeVehicleId, categoryId, "CD-123-EF", true);

        List<VehicleId> result = adapter.findConflictingVehicleIds(categoryId, period(PICKUP, PICKUP.plusDays(3)));

        assertThat(result).isEmpty();
    }

    @Test
    void findConflictingVehicleIds_categoryHasNoVehicles_returnsEmptyList() {
        VehicleCategoryId emptyCategoryId = VehicleCategoryId.of(UUID.randomUUID());
        persistCategory(emptyCategoryId, "Empty-" + emptyCategoryId.value());

        List<VehicleId> result = adapter.findConflictingVehicleIds(emptyCategoryId, period(PICKUP, PICKUP.plusDays(3)));

        assertThat(result).isEmpty();
    }

    @Test
    void databaseRejectsOverlappingConfirmedReservationsForSameVehicle() {
        insertReservation(vehicleId, PICKUP, PICKUP.plusDays(3), ReservationStatus.CONFIRMED);

        assertThatThrownBy(() -> insertReservation(vehicleId, PICKUP.plusDays(1), PICKUP.plusDays(2),
                ReservationStatus.CONFIRMED))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private DateRange period(ZonedDateTime start, ZonedDateTime end) {
        return new DateRange(start, end);
    }

    private void persistCategory(VehicleCategoryId id, String name) {
        VehicleCategoryJpaEntity entity = new VehicleCategoryJpaEntity();
        entity.id = id.value();
        entity.name = name;
        entity.description = name;
        entity.baseDailyRate = new BigDecimal("49.99");
        entity.depositAmount = new BigDecimal("300.00");
        entity.currency = Currency.getInstance("EUR").getCurrencyCode();
        entity.taxRate = new BigDecimal("0.20");
        entity.active = true;
        entityManager.persistAndFlush(entity);
    }

    private void persistVehicle(VehicleId id, VehicleCategoryId categoryId, String licensePlate, boolean active) {
        VehicleJpaEntity entity = new VehicleJpaEntity();
        entity.id = id.value();
        entity.licensePlate = licensePlate;
        entity.brand = "Toyota";
        entity.model = "Yaris";
        entity.year = 2024;
        entity.currentMileage = 1000;
        entity.status = VehicleStatus.AVAILABLE;
        entity.categoryId = categoryId.value();
        entity.active = active;
        entity.description = "Compact";
        entityManager.persistAndFlush(entity);
    }

    private void insertReservation(VehicleId vehicleId, ZonedDateTime pickup, ZonedDateTime returns,
                                   ReservationStatus status) {
        UUID customerId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO customers (id, first_name, last_name, email, status)
                VALUES (?, 'Ada', 'Lovelace', ?, 'ACTIVE')
                """, customerId, customerId + "@example.com");
        jdbc.update("""
                INSERT INTO reservations (id, reservation_number, customer_id, vehicle_id, pickup_datetime,
                    return_datetime, status, base_amount, currency, discount_amount, deposit_amount, tax_amount)
                VALUES (?, ?, ?, ?, ?, ?, ?, 100.00, 'EUR', 0.00, 0.00, 0.00)
                """,
                UUID.randomUUID(),
                "RES-" + UUID.randomUUID().toString().substring(0, 8),
                customerId,
                vehicleId.value(),
                OffsetDateTime.from(pickup),
                OffsetDateTime.from(returns),
                status.name());
    }
}
