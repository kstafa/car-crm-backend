package com.rentflow.contract.adapter.out.persistence;

import com.rentflow.AbstractJpaAdapterTest;
import com.rentflow.contract.DamageLiability;
import com.rentflow.contract.DamageReport;
import com.rentflow.contract.DamageReportStatus;
import com.rentflow.contract.DamageSeverity;
import com.rentflow.contract.model.DamageReportSummary;
import com.rentflow.contract.query.ListDamageReportsQuery;
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
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import({JpaDamageReportRepository.class, DamageReportJpaMapper.class})
class JpaDamageReportRepositoryTest extends AbstractJpaAdapterTest {

    private static final Currency EUR = Currency.getInstance("EUR");

    @Autowired
    private JpaDamageReportRepository repository;
    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void save_thenFindById_reportIsReconstituted() {
        DamageReport report = report(insertVehicle("DMG-001"));
        report.addPhoto("damage/a.jpg");

        repository.save(report);
        entityManager.flush();
        entityManager.clear();

        Optional<DamageReport> found = repository.findById(report.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getDamageDescription()).isEqualTo(report.getDamageDescription());
        assertThat(found.get().getPhotoKeys()).containsExactly("damage/a.jpg");
    }

    @Test
    void findAll_filterByStatus_returnsMatchingPage() {
        DamageReport open = report(insertVehicle("DMG-002"));
        DamageReport repair = report(insertVehicle("DMG-003"));
        repair.startRepair();
        repository.save(open);
        repository.save(repair);

        Page<DamageReportSummary> result = repository.findAll(new ListDamageReportsQuery(DamageReportStatus.OPEN,
                null, 0, 20));

        assertThat(result.getContent()).extracting(DamageReportSummary::id).containsExactly(open.getId());
    }

    @Test
    void findAll_filterByVehicleId_returnsMatchingReports() {
        VehicleId vehicleId = insertVehicle("DMG-004");
        DamageReport first = report(vehicleId);
        DamageReport second = report(insertVehicle("DMG-005"));
        repository.save(first);
        repository.save(second);

        Page<DamageReportSummary> result = repository.findAll(new ListDamageReportsQuery(null, vehicleId, 0, 20));

        assertThat(result.getContent()).extracting(DamageReportSummary::id).containsExactly(first.getId());
    }

    @Test
    void findByVehicleId_returnsAllReportsForVehicle() {
        VehicleId vehicleId = insertVehicle("DMG-006");
        DamageReport first = report(vehicleId);
        DamageReport second = report(vehicleId);
        repository.save(first);
        repository.save(second);

        List<DamageReportSummary> result = repository.findByVehicleId(vehicleId);

        assertThat(result).extracting(DamageReportSummary::id).containsExactlyInAnyOrder(first.getId(),
                second.getId());
    }

    private DamageReport report(VehicleId vehicleId) {
        return DamageReport.report(vehicleId, null, null, "Scratch on bumper", DamageSeverity.MINOR,
                DamageLiability.CUSTOMER, money("100.00"));
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
