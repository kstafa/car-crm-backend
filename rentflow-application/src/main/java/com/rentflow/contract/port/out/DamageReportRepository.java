package com.rentflow.contract.port.out;

import com.rentflow.contract.DamageReport;
import com.rentflow.contract.DamageReportId;
import com.rentflow.contract.model.DamageReportSummary;
import com.rentflow.contract.query.ListDamageReportsQuery;
import com.rentflow.shared.id.VehicleId;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface DamageReportRepository {
    void save(DamageReport report);

    Optional<DamageReport> findById(DamageReportId id);

    Page<DamageReportSummary> findAll(ListDamageReportsQuery query);

    List<DamageReportSummary> findByVehicleId(VehicleId vehicleId);
}
