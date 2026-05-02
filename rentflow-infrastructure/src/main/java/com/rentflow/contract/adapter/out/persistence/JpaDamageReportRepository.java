package com.rentflow.contract.adapter.out.persistence;

import com.rentflow.contract.DamageReport;
import com.rentflow.contract.DamageReportId;
import com.rentflow.contract.model.DamageReportSummary;
import com.rentflow.contract.port.out.DamageReportRepository;
import com.rentflow.contract.query.ListDamageReportsQuery;
import com.rentflow.shared.id.VehicleId;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@Primary
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JpaDamageReportRepository implements DamageReportRepository {
    private final SpringDataDamageReportRepo repo;
    private final DamageReportJpaMapper mapper;

    @Override
    @Transactional
    public void save(DamageReport report) {
        DamageReportJpaEntity entity = mapper.toJpa(report);
        repo.findById(entity.id).ifPresent(existing -> {
            entity.createdAt = existing.createdAt;
            entity.version = existing.version;
        });
        repo.save(entity);
    }

    @Override
    public Optional<DamageReport> findById(DamageReportId id) {
        return repo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Page<DamageReportSummary> findAll(ListDamageReportsQuery query) {
        return repo.findFiltered(query.status(), query.vehicleId() == null ? null : query.vehicleId().value(),
                PageRequest.of(query.page(), query.size())).map(mapper::toSummary);
    }

    @Override
    public List<DamageReportSummary> findByVehicleId(VehicleId vehicleId) {
        return repo.findByVehicleId(vehicleId.value()).stream().map(mapper::toSummary).toList();
    }
}
