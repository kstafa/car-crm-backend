package com.rentflow.fleet.adapter.out.persistence;

import com.rentflow.fleet.VehicleCategory;
import com.rentflow.fleet.model.CategorySummary;
import com.rentflow.fleet.port.out.VehicleCategoryRepository;
import com.rentflow.shared.id.VehicleCategoryId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaVehicleCategoryRepository implements VehicleCategoryRepository {
    private final SpringDataVehicleCategoryRepo repo;
    private final VehicleCategoryJpaMapper mapper;

    @Override
    public void save(VehicleCategory category) {
        VehicleCategoryJpaEntity entity = mapper.toJpa(category);
        repo.findById(entity.id).ifPresent(existing -> {
            entity.createdAt = existing.createdAt;
            entity.version = existing.version;
        });
        repo.save(entity);
    }

    @Override
    public Optional<VehicleCategory> findById(VehicleCategoryId id) {
        return repo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<VehicleCategory> findByName(String name) {
        return repo.findByName(name).map(mapper::toDomain);
    }

    @Override
    public List<CategorySummary> findAllActive() {
        return repo.findByActiveTrue().stream().map(mapper::toSummary).toList();
    }
}
