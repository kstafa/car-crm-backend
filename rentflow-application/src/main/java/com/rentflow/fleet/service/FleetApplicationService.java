package com.rentflow.fleet.service;

import com.rentflow.fleet.Vehicle;
import com.rentflow.fleet.VehicleCategory;
import com.rentflow.fleet.command.CreateCategoryCommand;
import com.rentflow.fleet.command.RegisterVehicleCommand;
import com.rentflow.fleet.command.UpdateVehicleStatusCommand;
import com.rentflow.fleet.model.AvailableVehicle;
import com.rentflow.fleet.model.CategorySummary;
import com.rentflow.fleet.model.VehicleDetail;
import com.rentflow.fleet.model.VehicleSummary;
import com.rentflow.fleet.port.in.CreateCategoryUseCase;
import com.rentflow.fleet.port.in.FindAvailableVehiclesUseCase;
import com.rentflow.fleet.port.in.GetVehicleUseCase;
import com.rentflow.fleet.port.in.ListCategoriesUseCase;
import com.rentflow.fleet.port.in.ListVehiclesUseCase;
import com.rentflow.fleet.port.in.RegisterVehicleUseCase;
import com.rentflow.fleet.port.in.UpdateVehicleStatusUseCase;
import com.rentflow.fleet.port.out.VehicleCategoryRepository;
import com.rentflow.fleet.port.out.VehicleRepository;
import com.rentflow.fleet.query.FindAvailableVehiclesQuery;
import com.rentflow.fleet.query.ListVehiclesQuery;
import com.rentflow.reservation.DateRange;
import com.rentflow.reservation.port.out.AvailabilityCachePort;
import com.rentflow.reservation.port.out.VehicleAvailabilityPort;
import com.rentflow.shared.AuditEntry;
import com.rentflow.shared.DomainEvent;
import com.rentflow.shared.DomainException;
import com.rentflow.shared.ResourceNotFoundException;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;
import com.rentflow.shared.port.out.AuditLogPort;
import com.rentflow.shared.port.out.DomainEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class FleetApplicationService implements RegisterVehicleUseCase, UpdateVehicleStatusUseCase,
        GetVehicleUseCase, ListVehiclesUseCase, FindAvailableVehiclesUseCase, CreateCategoryUseCase,
        ListCategoriesUseCase {

    private final VehicleRepository vehicleRepository;
    private final VehicleCategoryRepository categoryRepository;
    private final VehicleAvailabilityPort availabilityPort;
    private final AvailabilityCachePort cachePort;
    private final DomainEventPublisher eventPublisher;
    private final AuditLogPort auditLogPort;

    public FleetApplicationService(VehicleRepository vehicleRepository, VehicleCategoryRepository categoryRepository,
                                   VehicleAvailabilityPort availabilityPort, AvailabilityCachePort cachePort,
                                   DomainEventPublisher eventPublisher, AuditLogPort auditLogPort) {
        this.vehicleRepository = vehicleRepository;
        this.categoryRepository = categoryRepository;
        this.availabilityPort = availabilityPort;
        this.cachePort = cachePort;
        this.eventPublisher = eventPublisher;
        this.auditLogPort = auditLogPort;
    }

    @Override
    public VehicleId register(RegisterVehicleCommand cmd) {
        VehicleCategory category = loadCategory(cmd.categoryId());
        if (!category.isActive()) {
            throw new DomainException("Category is not active");
        }

        Vehicle vehicle = Vehicle.register(cmd.licensePlate(), cmd.brand(), cmd.model(), cmd.year(),
                cmd.categoryId(), cmd.initialMileage(), cmd.description());
        vehicleRepository.save(vehicle);
        publishEvents(vehicle.pullDomainEvents());
        auditLogPort.log(AuditEntry.of("REGISTER_VEHICLE", vehicle.getId(), cmd.registeredBy()));
        return vehicle.getId();
    }

    @Override
    public void update(UpdateVehicleStatusCommand cmd) {
        Vehicle vehicle = vehicleRepository.findById(cmd.vehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + cmd.vehicleId().value()));

        switch (cmd.newStatus()) {
            case AVAILABLE -> vehicle.markAsAvailable();
            case RENTED -> vehicle.markAsRented();
            case MAINTENANCE -> vehicle.sendToMaintenance();
            case OUT_OF_SERVICE -> vehicle.putOutOfService();
        }

        vehicleRepository.save(vehicle);
        publishEvents(vehicle.pullDomainEvents());
        auditLogPort.log(AuditEntry.of("UPDATE_VEHICLE_STATUS", vehicle.getId(), cmd.updatedBy()));
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleDetail get(VehicleId id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + id.value()));
        String categoryName = categoryRepository.findById(vehicle.getCategoryId())
                .map(VehicleCategory::getName)
                .orElse(null);
        return toDetail(vehicle, categoryName);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleSummary> list(ListVehiclesQuery q) {
        return vehicleRepository.findAll(q);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailableVehicle> find(FindAvailableVehiclesQuery q) {
        VehicleCategory category = loadCategory(q.categoryId());
        DateRange period = q.toDateRange();
        List<VehicleId> availableIds = cachePort.get(q.categoryId(), period)
                .orElseGet(() -> findAndCacheAvailableIds(q.categoryId(), period));
        return availableIds.stream()
                .map(vehicleRepository::findById)
                .flatMap(Optional -> Optional.stream())
                .map(vehicle -> toAvailableVehicle(vehicle, category))
                .toList();
    }

    @Override
    public VehicleCategoryId create(CreateCategoryCommand cmd) {
        categoryRepository.findByName(cmd.name())
                .filter(VehicleCategory::isActive)
                .ifPresent(category -> {
                    throw new DomainException("Category name already in use");
                });

        VehicleCategory category = VehicleCategory.create(cmd.name(), cmd.description(), cmd.baseDailyRate(),
                cmd.depositAmount(), cmd.taxRate());
        categoryRepository.save(category);
        publishEvents(category.pullDomainEvents());
        auditLogPort.log(AuditEntry.of("CREATE_VEHICLE_CATEGORY", category.getId(), cmd.createdBy()));
        return category.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategorySummary> list() {
        return categoryRepository.findAllActive();
    }

    private List<VehicleId> findAndCacheAvailableIds(VehicleCategoryId categoryId, DateRange period) {
        List<Vehicle> activeVehicles = vehicleRepository.findActiveByCategoryId(categoryId);
        Set<VehicleId> conflictingIds = new HashSet<>(availabilityPort.findConflictingVehicleIds(categoryId, period));
        List<VehicleId> availableIds = activeVehicles.stream()
                .map(Vehicle::getId)
                .filter(id -> !conflictingIds.contains(id))
                .toList();
        cachePort.put(categoryId, period, availableIds);
        return availableIds;
    }

    private VehicleCategory loadCategory(VehicleCategoryId categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    private static VehicleDetail toDetail(Vehicle vehicle, String categoryName) {
        return new VehicleDetail(
                vehicle.getId(),
                vehicle.getLicensePlate(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getYear(),
                vehicle.getStatus(),
                vehicle.getCategoryId(),
                categoryName,
                vehicle.getCurrentMileage(),
                vehicle.isActive(),
                vehicle.getDescription(),
                List.copyOf(vehicle.getPhotoKeys())
        );
    }

    private static AvailableVehicle toAvailableVehicle(Vehicle vehicle, VehicleCategory category) {
        String thumbnailKey = vehicle.getPhotoKeys().isEmpty() ? null : vehicle.getPhotoKeys().getFirst();
        return new AvailableVehicle(vehicle.getId(), vehicle.getLicensePlate(), vehicle.getBrand(), vehicle.getModel(),
                vehicle.getYear(), category.getName(), category.getBaseDailyRate(), thumbnailKey);
    }

    private void publishEvents(List<DomainEvent> events) {
        events.forEach(eventPublisher::publish);
    }
}
