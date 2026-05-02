package com.rentflow.fleet.adapter.out.persistence;

import com.rentflow.AbstractJpaAdapterTest;
import com.rentflow.fleet.Vehicle;
import com.rentflow.fleet.VehicleStatus;
import com.rentflow.fleet.model.VehicleSummary;
import com.rentflow.fleet.query.ListVehiclesQuery;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import({JpaVehicleRepository.class, VehicleJpaMapper.class})
class JpaVehicleRepositoryTest extends AbstractJpaAdapterTest {

    @Autowired
    private JpaVehicleRepository repository;
    @Autowired
    private TestEntityManager entityManager;

    private VehicleCategoryId economyCategoryId;
    private VehicleCategoryId suvCategoryId;

    @BeforeEach
    void setUp() {
        economyCategoryId = VehicleCategoryId.of(UUID.randomUUID());
        suvCategoryId = VehicleCategoryId.of(UUID.randomUUID());
        persistCategory(economyCategoryId, "Economy-" + economyCategoryId.value(), true);
        persistCategory(suvCategoryId, "SUV-" + suvCategoryId.value(), true);
    }

    @Test
    void save_thenFindById_domainAggregateIsReconstituted() {
        Vehicle vehicle = Vehicle.register("AB-123-CD", "Toyota", "Yaris", 2024, economyCategoryId, 1200,
                "Compact");
        vehicle.addPhoto("vehicles/ab.jpg");

        repository.save(vehicle);
        entityManager.flush();
        entityManager.clear();

        Optional<Vehicle> found = repository.findById(vehicle.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getLicensePlate()).isEqualTo("AB-123-CD");
        assertThat(found.get().getPhotoKeys()).containsExactly("vehicles/ab.jpg");
    }

    @Test
    void findActiveByCategoryId_returnsOnlyActiveVehiclesForCategory() {
        Vehicle economy = vehicle("AA-111-AA", economyCategoryId);
        Vehicle suv = vehicle("BB-111-BB", suvCategoryId);
        repository.save(economy);
        repository.save(suv);

        List<Vehicle> result = repository.findActiveByCategoryId(economyCategoryId);

        assertThat(result).extracting(Vehicle::getId).containsExactly(economy.getId());
    }

    @Test
    void findActiveByCategoryId_excludesInactiveVehicles() {
        Vehicle active = vehicle("AA-111-AA", economyCategoryId);
        Vehicle inactive = vehicle("AA-222-AA", economyCategoryId);
        inactive.deactivate();
        repository.save(active);
        repository.save(inactive);

        List<Vehicle> result = repository.findActiveByCategoryId(economyCategoryId);

        assertThat(result).extracting(Vehicle::getId).containsExactly(active.getId());
    }

    @Test
    void findAll_filterByStatus_returnsMatchingVehicles() {
        Vehicle available = vehicle("AA-111-AA", economyCategoryId);
        Vehicle maintenance = vehicle("AA-222-AA", economyCategoryId);
        maintenance.sendToMaintenance();
        repository.save(available);
        repository.save(maintenance);

        Page<VehicleSummary> result = repository.findAll(new ListVehiclesQuery(VehicleStatus.MAINTENANCE, null,
                true, 0, 20, "licensePlate"));

        assertThat(result.getContent()).extracting(VehicleSummary::id).containsExactly(maintenance.getId());
    }

    @Test
    void findAll_filterByCategory_returnsMatchingVehicles() {
        Vehicle economy = vehicle("AA-111-AA", economyCategoryId);
        Vehicle suv = vehicle("BB-111-BB", suvCategoryId);
        repository.save(economy);
        repository.save(suv);

        Page<VehicleSummary> result = repository.findAll(new ListVehiclesQuery(null, suvCategoryId, true, 0, 20,
                "licensePlate"));

        assertThat(result.getContent()).extracting(VehicleSummary::id).containsExactly(suv.getId());
    }

    private static Vehicle vehicle(String licensePlate, VehicleCategoryId categoryId) {
        return Vehicle.register(licensePlate, "Toyota", "Yaris", 2024, categoryId, 1000);
    }

    private void persistCategory(VehicleCategoryId id, String name, boolean active) {
        VehicleCategoryJpaEntity entity = new VehicleCategoryJpaEntity();
        entity.id = id.value();
        entity.name = name;
        entity.description = name;
        entity.baseDailyRate = new BigDecimal("49.99");
        entity.depositAmount = new BigDecimal("300.00");
        entity.currency = Currency.getInstance("EUR").getCurrencyCode();
        entity.taxRate = new BigDecimal("0.20");
        entity.active = active;
        entityManager.persistAndFlush(entity);
    }
}
