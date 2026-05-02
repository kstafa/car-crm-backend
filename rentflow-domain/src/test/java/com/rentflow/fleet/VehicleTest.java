package com.rentflow.fleet;

import com.rentflow.shared.DomainException;
import com.rentflow.shared.id.VehicleCategoryId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VehicleTest {

    private static final VehicleCategoryId CATEGORY_ID = VehicleCategoryId.generate();

    @Test
    void register_validParams_setsAvailableStatusAndRegistersEvent() {
        Vehicle vehicle = vehicle();

        assertEquals(VehicleStatus.AVAILABLE, vehicle.getStatus());
        assertEquals(1, vehicle.pullDomainEvents().size());
    }

    @Test
    void markAsRented_whenAvailable_setsRented() {
        Vehicle vehicle = vehicle();

        vehicle.markAsRented();

        assertEquals(VehicleStatus.RENTED, vehicle.getStatus());
    }

    @Test
    void markAsRented_whenNotAvailable_throwsDomainException() {
        Vehicle vehicle = vehicle();
        vehicle.sendToMaintenance();

        assertThrows(DomainException.class, vehicle::markAsRented);
    }

    @Test
    void markAsAvailable_fromMaintenance_setsAvailableAndRegistersEvent() {
        Vehicle vehicle = vehicle();
        vehicle.sendToMaintenance();
        vehicle.pullDomainEvents();

        vehicle.markAsAvailable();

        assertEquals(VehicleStatus.AVAILABLE, vehicle.getStatus());
        assertEquals(VehicleAvailableEvent.class, vehicle.pullDomainEvents().get(0).getClass());
    }

    @Test
    void sendToMaintenance_whenAvailable_setsMaintenance() {
        Vehicle vehicle = vehicle();

        vehicle.sendToMaintenance();

        assertEquals(VehicleStatus.MAINTENANCE, vehicle.getStatus());
    }

    @Test
    void sendToMaintenance_whenRented_throwsDomainException() {
        Vehicle vehicle = vehicle();
        vehicle.markAsRented();

        assertThrows(DomainException.class, vehicle::sendToMaintenance);
    }

    @Test
    void updateMileage_higherValue_updatesSuccessfully() {
        Vehicle vehicle = vehicle();

        vehicle.updateMileage(1250);

        assertEquals(1250, vehicle.getCurrentMileage());
    }

    @Test
    void updateMileage_lowerValue_throwsDomainException() {
        Vehicle vehicle = vehicle();

        assertThrows(DomainException.class, () -> vehicle.updateMileage(999));
    }

    @Test
    void updateMileage_sameValue_allowed() {
        Vehicle vehicle = vehicle();

        vehicle.updateMileage(1000);

        assertEquals(1000, vehicle.getCurrentMileage());
    }

    @Test
    void deactivate_whenAvailable_setsInactive() {
        Vehicle vehicle = vehicle();

        vehicle.deactivate();

        assertFalse(vehicle.isActive());
    }

    @Test
    void deactivate_whenRented_throwsDomainException() {
        Vehicle vehicle = vehicle();
        vehicle.markAsRented();

        assertThrows(DomainException.class, vehicle::deactivate);
    }

    @Test
    void addPhoto_validKey_appendsToList() {
        Vehicle vehicle = vehicle();

        vehicle.addPhoto("vehicles/a.jpg");

        assertEquals(List.of("vehicles/a.jpg"), vehicle.getPhotoKeys());
    }

    @Test
    void addPhoto_blankKey_throwsDomainException() {
        Vehicle vehicle = vehicle();

        assertThrows(DomainException.class, () -> vehicle.addPhoto(" "));
    }

    @Test
    void removePhoto_existingKey_removesFromList() {
        Vehicle vehicle = vehicle();
        vehicle.addPhoto("vehicles/a.jpg");

        vehicle.removePhoto("vehicles/a.jpg");

        assertTrue(vehicle.getPhotoKeys().isEmpty());
    }

    @Test
    void removePhoto_unknownKey_noOpNoException() {
        Vehicle vehicle = vehicle();

        assertDoesNotThrow(() -> vehicle.removePhoto("missing.jpg"));
        assertTrue(vehicle.getPhotoKeys().isEmpty());
    }

    private static Vehicle vehicle() {
        return Vehicle.register("AA-123-AA", "Toyota", "Yaris", 2024, CATEGORY_ID, 1000);
    }
}
