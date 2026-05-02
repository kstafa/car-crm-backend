package com.rentflow.contract.adapter.out.persistence;

import com.rentflow.AbstractJpaAdapterTest;
import com.rentflow.contract.Contract;
import com.rentflow.contract.ContractStatus;
import com.rentflow.contract.Inspection;
import com.rentflow.contract.InspectionChecklist;
import com.rentflow.contract.model.ContractSummary;
import com.rentflow.contract.query.ListContractsQuery;
import com.rentflow.shared.FuelLevel;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import({JpaContractRepository.class, ContractJpaMapper.class})
class JpaContractRepositoryTest extends AbstractJpaAdapterTest {

    private static final ZonedDateTime PICKUP = ZonedDateTime.of(2026, 7, 1, 9, 0, 0, 0, ZoneId.of("UTC"));

    @Autowired
    private JpaContractRepository repository;
    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void save_thenFindById_contractIsReconstituted() {
        Fixtures fixtures = insertFixtures("CTR-001");
        Contract contract = contract(fixtures);

        repository.save(contract);
        entityManager.flush();
        entityManager.clear();

        Optional<Contract> found = repository.findById(contract.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getContractNumber()).isEqualTo(contract.getContractNumber());
        assertThat(found.get().getReservationId()).isEqualTo(fixtures.reservationId());
    }

    @Test
    void findByReservationId_existingReservation_returnsContract() {
        Fixtures fixtures = insertFixtures("CTR-002");
        Contract contract = contract(fixtures);
        repository.save(contract);

        Optional<Contract> found = repository.findByReservationId(fixtures.reservationId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(contract.getId());
    }

    @Test
    void findByReservationId_unknownReservation_returnsEmpty() {
        Optional<Contract> found = repository.findByReservationId(ReservationId.generate());

        assertThat(found).isEmpty();
    }

    @Test
    void findActive_onlyActiveContracts_returned() {
        Contract active = contract(insertFixtures("CTR-003"));
        Contract completed = completedContract(insertFixtures("CTR-004"));
        repository.save(active);
        repository.save(completed);

        List<ContractSummary> result = repository.findActive();

        assertThat(result).extracting(ContractSummary::id).containsExactly(active.getId());
    }

    @Test
    void findActive_completedContractExcluded() {
        Contract completed = completedContract(insertFixtures("CTR-005"));
        repository.save(completed);

        List<ContractSummary> result = repository.findActive();

        assertThat(result).isEmpty();
    }

    @Test
    void findFiltered_byStatus_returnsMatchingPage() {
        Contract active = contract(insertFixtures("CTR-006"));
        Contract completed = completedContract(insertFixtures("CTR-007"));
        repository.save(active);
        repository.save(completed);

        Page<ContractSummary> result = repository.findAll(new ListContractsQuery(ContractStatus.COMPLETED, null,
                null, 0, 20));

        assertThat(result.getContent()).extracting(ContractSummary::id).containsExactly(completed.getId());
    }

    @Test
    void findFiltered_byVehicleId_returnsMatchingContracts() {
        Contract first = contract(insertFixtures("CTR-008"));
        Contract second = contract(insertFixtures("CTR-009"));
        repository.save(first);
        repository.save(second);

        Page<ContractSummary> result = repository.findAll(new ListContractsQuery(null, first.getVehicleId(),
                null, 0, 20));

        assertThat(result.getContent()).extracting(ContractSummary::id).containsExactly(first.getId());
    }

    @Test
    void save_withPreAndPostInspection_persistsBothInspections() {
        Contract contract = completedContract(insertFixtures("CTR-010"));

        repository.save(contract);
        entityManager.flush();
        entityManager.clear();

        Optional<Contract> found = repository.findById(contract.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getPreInspection()).isNotNull();
        assertThat(found.get().getPostInspection()).isNotNull();
        assertThat(found.get().getPreInspection().fuelLevel()).isEqualTo(FuelLevel.FULL);
        assertThat(found.get().getPostInspection().checklist().hasDamage()).isFalse();
    }

    private Contract completedContract(Fixtures fixtures) {
        Contract contract = contract(fixtures);
        contract.recordPickup(inspection(Inspection.InspectionType.PRE), PICKUP);
        contract.recordReturn(inspection(Inspection.InspectionType.POST), PICKUP.plusDays(3));
        return contract;
    }

    private Contract contract(Fixtures fixtures) {
        return Contract.open(fixtures.reservationId(), fixtures.customerId(), fixtures.vehicleId(), PICKUP,
                PICKUP.plusDays(3));
    }

    private static Inspection inspection(Inspection.InspectionType type) {
        return new Inspection(type, InspectionChecklist.allOk(), FuelLevel.FULL, 1000, List.of("a.jpg"),
                Instant.now(), StaffId.generate());
    }

    private Fixtures insertFixtures(String licensePlate) {
        CustomerId customerId = CustomerId.of(UUID.randomUUID());
        jdbc.update("""
                INSERT INTO customers (id, first_name, last_name, email, status)
                VALUES (?, 'Ada', 'Lovelace', ?, 'ACTIVE')
                """, customerId.value(), customerId.value() + "@example.com");
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
        ReservationId reservationId = ReservationId.of(UUID.randomUUID());
        jdbc.update("""
                INSERT INTO reservations
                    (id, reservation_number, customer_id, vehicle_id, pickup_datetime, return_datetime, status,
                     base_amount, currency, discount_amount, deposit_amount, tax_amount)
                VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', 300.00, 'EUR', 0.00, 0.00, 0.00)
                """, reservationId.value(), "RES-" + licensePlate, customerId.value(), vehicleId.value(),
                OffsetDateTime.from(PICKUP), OffsetDateTime.from(PICKUP.plusDays(3)));
        return new Fixtures(customerId, vehicleId, reservationId);
    }

    private record Fixtures(CustomerId customerId, VehicleId vehicleId, ReservationId reservationId) {
    }
}
