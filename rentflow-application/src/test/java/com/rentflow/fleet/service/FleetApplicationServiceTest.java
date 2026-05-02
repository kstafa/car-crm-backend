package com.rentflow.fleet.service;

import com.rentflow.fleet.Vehicle;
import com.rentflow.fleet.VehicleCategory;
import com.rentflow.fleet.VehicleRegisteredEvent;
import com.rentflow.fleet.VehicleStatus;
import com.rentflow.fleet.command.CreateCategoryCommand;
import com.rentflow.fleet.command.RegisterVehicleCommand;
import com.rentflow.fleet.command.UpdateVehicleStatusCommand;
import com.rentflow.fleet.model.AvailableVehicle;
import com.rentflow.fleet.model.VehicleDetail;
import com.rentflow.fleet.port.out.VehicleCategoryRepository;
import com.rentflow.fleet.port.out.VehicleRepository;
import com.rentflow.fleet.query.FindAvailableVehiclesQuery;
import com.rentflow.reservation.DateRange;
import com.rentflow.reservation.port.out.AvailabilityCachePort;
import com.rentflow.reservation.port.out.VehicleAvailabilityPort;
import com.rentflow.shared.DomainException;
import com.rentflow.shared.ResourceNotFoundException;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;
import com.rentflow.shared.money.Money;
import com.rentflow.shared.port.out.AuditLogPort;
import com.rentflow.shared.port.out.DomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FleetApplicationServiceTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final ZonedDateTime PICKUP = ZonedDateTime.of(2026, 7, 1, 9, 0, 0, 0, ZoneOffset.UTC);

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private VehicleCategoryRepository categoryRepository;
    @Mock
    private VehicleAvailabilityPort availabilityPort;
    @Mock
    private AvailabilityCachePort cachePort;
    @Mock
    private DomainEventPublisher eventPublisher;
    @Mock
    private AuditLogPort auditLogPort;

    private FleetApplicationService service;
    private VehicleCategoryId categoryId;
    private StaffId staffId;

    @BeforeEach
    void setUp() {
        service = new FleetApplicationService(vehicleRepository, categoryRepository, availabilityPort, cachePort,
                eventPublisher, auditLogPort);
        categoryId = VehicleCategoryId.generate();
        staffId = StaffId.generate();
    }

    @Test
    void register_validParams_savesAndReturnsId() {
        stubCategory(activeCategory());

        VehicleId id = service.register(registerCommand());

        ArgumentCaptor<Vehicle> captor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository).save(captor.capture());
        assertEquals(id, captor.getValue().getId());
        assertEquals(VehicleStatus.AVAILABLE, captor.getValue().getStatus());
    }

    @Test
    void register_categoryNotFound_throwsResourceNotFoundException() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.register(registerCommand()));
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void register_categoryInactive_throwsDomainException() {
        VehicleCategory category = activeCategory();
        category.deactivate();
        stubCategory(category);

        assertThrows(DomainException.class, () -> service.register(registerCommand()));
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void register_validParams_publishesVehicleRegisteredEvent() {
        stubCategory(activeCategory());

        service.register(registerCommand());

        verify(eventPublisher).publish(any(VehicleRegisteredEvent.class));
    }

    @Test
    void update_toMaintenance_callsSendToMaintenance() {
        Vehicle vehicle = vehicle();
        when(vehicleRepository.findById(vehicle.getId())).thenReturn(Optional.of(vehicle));

        service.update(new UpdateVehicleStatusCommand(vehicle.getId(), VehicleStatus.MAINTENANCE, staffId));

        assertEquals(VehicleStatus.MAINTENANCE, vehicle.getStatus());
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    void update_toAvailable_callsMarkAsAvailable() {
        Vehicle vehicle = vehicle();
        vehicle.sendToMaintenance();
        when(vehicleRepository.findById(vehicle.getId())).thenReturn(Optional.of(vehicle));

        service.update(new UpdateVehicleStatusCommand(vehicle.getId(), VehicleStatus.AVAILABLE, staffId));

        assertEquals(VehicleStatus.AVAILABLE, vehicle.getStatus());
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    void update_vehicleNotFound_throwsResourceNotFoundException() {
        VehicleId vehicleId = VehicleId.generate();
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.update(new UpdateVehicleStatusCommand(vehicleId, VehicleStatus.AVAILABLE, staffId)));
    }

    @Test
    void get_exists_returnsMappedDetail() {
        Vehicle vehicle = vehicle();
        when(vehicleRepository.findById(vehicle.getId())).thenReturn(Optional.of(vehicle));
        stubCategory(activeCategory());

        VehicleDetail detail = service.get(vehicle.getId());

        assertEquals(vehicle.getId(), detail.id());
        assertEquals(vehicle.getLicensePlate(), detail.licensePlate());
        assertEquals("Economy", detail.categoryName());
    }

    @Test
    void get_notFound_throwsResourceNotFoundException() {
        VehicleId vehicleId = VehicleId.generate();
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.get(vehicleId));
    }

    @Test
    void findAvailable_cacheHit_returnsCachedResultWithoutQueryingDb() {
        Vehicle vehicle = vehicle();
        FindAvailableVehiclesQuery query = availabilityQuery();
        stubCategory(activeCategory());
        when(cachePort.get(eq(categoryId), any(DateRange.class))).thenReturn(Optional.of(List.of(vehicle.getId())));
        when(vehicleRepository.findById(vehicle.getId())).thenReturn(Optional.of(vehicle));

        List<AvailableVehicle> result = service.find(query);

        assertEquals(List.of(vehicle.getId()), result.stream().map(AvailableVehicle::id).toList());
        verify(vehicleRepository, never()).findActiveByCategoryId(any());
        verify(availabilityPort, never()).findConflictingVehicleIds(any(), any());
    }

    @Test
    void findAvailable_cacheMiss_queriesDbAndPopulatesCache() {
        Vehicle vehicle = vehicle();
        FindAvailableVehiclesQuery query = availabilityQuery();
        stubCategory(activeCategory());
        when(cachePort.get(eq(categoryId), any(DateRange.class))).thenReturn(Optional.empty());
        when(vehicleRepository.findActiveByCategoryId(categoryId)).thenReturn(List.of(vehicle));
        when(availabilityPort.findConflictingVehicleIds(eq(categoryId), any(DateRange.class))).thenReturn(List.of());
        when(vehicleRepository.findById(vehicle.getId())).thenReturn(Optional.of(vehicle));

        List<AvailableVehicle> result = service.find(query);

        assertEquals(1, result.size());
        verify(vehicleRepository).findActiveByCategoryId(categoryId);
        verify(cachePort).put(eq(categoryId), any(DateRange.class), eq(List.of(vehicle.getId())));
    }

    @Test
    void findAvailable_cacheMiss_excludesConflictingVehicles() {
        Vehicle free = vehicle();
        Vehicle conflicting = vehicle("BB-123-BB");
        stubCategory(activeCategory());
        when(cachePort.get(eq(categoryId), any(DateRange.class))).thenReturn(Optional.empty());
        when(vehicleRepository.findActiveByCategoryId(categoryId)).thenReturn(List.of(free, conflicting));
        when(availabilityPort.findConflictingVehicleIds(eq(categoryId), any(DateRange.class)))
                .thenReturn(List.of(conflicting.getId()));
        when(vehicleRepository.findById(free.getId())).thenReturn(Optional.of(free));

        List<AvailableVehicle> result = service.find(availabilityQuery());

        assertEquals(List.of(free.getId()), result.stream().map(AvailableVehicle::id).toList());
    }

    @Test
    void findAvailable_allVehiclesConflicting_returnsEmptyList() {
        Vehicle conflicting = vehicle();
        stubCategory(activeCategory());
        when(cachePort.get(eq(categoryId), any(DateRange.class))).thenReturn(Optional.empty());
        when(vehicleRepository.findActiveByCategoryId(categoryId)).thenReturn(List.of(conflicting));
        when(availabilityPort.findConflictingVehicleIds(eq(categoryId), any(DateRange.class)))
                .thenReturn(List.of(conflicting.getId()));

        List<AvailableVehicle> result = service.find(availabilityQuery());

        assertTrue(result.isEmpty());
    }

    @Test
    void createCategory_uniqueName_savesAndReturnsId() {
        when(categoryRepository.findByName("Economy")).thenReturn(Optional.empty());

        VehicleCategoryId id = service.create(createCategoryCommand());

        ArgumentCaptor<VehicleCategory> captor = ArgumentCaptor.forClass(VehicleCategory.class);
        verify(categoryRepository).save(captor.capture());
        assertEquals(id, captor.getValue().getId());
    }

    @Test
    void createCategory_duplicateName_throwsDomainException() {
        VehicleCategory existing = activeCategory();
        when(categoryRepository.findByName("Economy")).thenReturn(Optional.of(existing));

        assertThrows(DomainException.class, () -> service.create(createCategoryCommand()));
        verify(categoryRepository, never()).save(any());
    }

    private RegisterVehicleCommand registerCommand() {
        return new RegisterVehicleCommand("AA-123-AA", "Toyota", "Yaris", 2024, categoryId, 1000,
                "Compact city car", staffId);
    }

    private CreateCategoryCommand createCategoryCommand() {
        return new CreateCategoryCommand("Economy", "Small cars", money("49.99"), money("300.00"),
                new BigDecimal("0.20"), staffId);
    }

    private FindAvailableVehiclesQuery availabilityQuery() {
        return new FindAvailableVehiclesQuery(categoryId, PICKUP, PICKUP.plusDays(3));
    }

    private Vehicle vehicle() {
        return vehicle("AA-123-AA");
    }

    private Vehicle vehicle(String licensePlate) {
        return Vehicle.register(licensePlate, "Toyota", "Yaris", 2024, categoryId, 1000, "Compact city car");
    }

    private VehicleCategory activeCategory() {
        return VehicleCategory.reconstitute(categoryId, "Economy", "Small cars", money("49.99"), money("300.00"),
                new BigDecimal("0.20"), true);
    }

    private void stubCategory(VehicleCategory category) {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), EUR);
    }
}
