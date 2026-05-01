package com.rentflow.fleet;

import com.rentflow.shared.AggregateRoot;
import com.rentflow.shared.DomainException;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;

import java.util.Objects;

public final class Vehicle extends AggregateRoot {

    private final VehicleId id;
    private final String licensePlate;
    private final String brand;
    private final String model;
    private final int year;
    private final VehicleCategoryId categoryId;
    private int currentMileage;
    private VehicleStatus status;
    private boolean active;

    private Vehicle(VehicleId id, String licensePlate, String brand, String model, int year,
                    VehicleCategoryId categoryId, int currentMileage, VehicleStatus status, boolean active) {
        this.id = id;
        this.licensePlate = licensePlate;
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.categoryId = categoryId;
        this.currentMileage = currentMileage;
        this.status = status;
        this.active = active;
    }

    public static Vehicle register(String licensePlate, String brand, String model, int year,
                                   VehicleCategoryId categoryId, int initialMileage) {
        Vehicle vehicle = new Vehicle(
                VehicleId.generate(),
                Objects.requireNonNull(licensePlate),
                Objects.requireNonNull(brand),
                Objects.requireNonNull(model),
                year,
                Objects.requireNonNull(categoryId),
                initialMileage,
                VehicleStatus.AVAILABLE,
                true
        );
        vehicle.registerEvent(new VehicleRegisteredEvent(vehicle.id));
        return vehicle;
    }

    public void markAsRented() {
        if (status != VehicleStatus.AVAILABLE) {
            throw new DomainException("Vehicle must be available before it can be rented");
        }
        status = VehicleStatus.RENTED;
    }

    public void markAsAvailable() {
        status = VehicleStatus.AVAILABLE;
        registerEvent(new VehicleAvailableEvent(id));
    }

    public void sendToMaintenance() {
        if (status == VehicleStatus.RENTED) {
            throw new DomainException("Rented vehicle cannot be sent to maintenance");
        }
        status = VehicleStatus.MAINTENANCE;
        registerEvent(new VehicleMarkedForMaintenanceEvent(id));
    }

    public void updateMileage(int newMileage) {
        if (newMileage < currentMileage) {
            throw new DomainException("Mileage cannot decrease");
        }
        currentMileage = newMileage;
    }

    public void deactivate() {
        if (status == VehicleStatus.RENTED) {
            throw new DomainException("Rented vehicle cannot be deactivated");
        }
        active = false;
        registerEvent(new VehicleDeactivatedEvent(id));
    }

    public VehicleId getId() {
        return id;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public int getYear() {
        return year;
    }

    public VehicleCategoryId getCategoryId() {
        return categoryId;
    }

    public int getCurrentMileage() {
        return currentMileage;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return active;
    }
}
