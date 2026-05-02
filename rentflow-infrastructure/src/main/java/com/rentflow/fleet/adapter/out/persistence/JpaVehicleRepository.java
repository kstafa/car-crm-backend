package com.rentflow.fleet.adapter.out.persistence;

import com.rentflow.fleet.Vehicle;
import com.rentflow.fleet.model.VehicleSummary;
import com.rentflow.fleet.port.out.VehicleRepository;
import com.rentflow.fleet.query.ListVehiclesQuery;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Primary
@RequiredArgsConstructor
public class JpaVehicleRepository implements VehicleRepository {
    private final SpringDataVehicleRepo repo;
    private final VehicleJpaMapper mapper;

    @Override
    public void save(Vehicle vehicle) {
        VehicleJpaEntity entity = mapper.toJpa(vehicle);
        repo.findById(entity.id).ifPresent(existing -> entity.version = existing.version);
        repo.save(entity);
    }

    @Override
    public Optional<Vehicle> findById(VehicleId id) {
        return repo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<Vehicle> findActiveByCategoryId(VehicleCategoryId categoryId) {
        return repo.findByCategoryIdAndActiveTrue(categoryId.value())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<VehicleSummary> findAll(ListVehiclesQuery query) {
        PageRequest pageable = PageRequest.of(query.page(), query.size(), sort(query.sortBy()));
        return repo.findFiltered(query.status(), query.categoryId() != null ? query.categoryId().value() : null,
                query.activeOnly(), pageable).map(mapper::toSummary);
    }

    private static Sort sort(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return Sort.by("licensePlate").ascending();
        }
        return Sort.by(sortBy).ascending();
    }
}
