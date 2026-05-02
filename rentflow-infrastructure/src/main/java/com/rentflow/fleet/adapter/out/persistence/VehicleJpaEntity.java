package com.rentflow.fleet.adapter.out.persistence;

import com.rentflow.fleet.VehicleStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "vehicles")
class VehicleJpaEntity {
    @Id
    UUID id;
    @Column(unique = true, nullable = false, length = 20)
    String licensePlate;
    @Column(nullable = false, length = 100)
    String brand;
    @Column(nullable = false, length = 100)
    String model;
    int year;
    int currentMileage;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    VehicleStatus status;
    UUID categoryId;
    boolean active;
    @Column(columnDefinition = "text")
    String description;
    @ElementCollection
    @CollectionTable(name = "vehicle_photos", joinColumns = @JoinColumn(name = "vehicle_id"))
    @Column(name = "photo_key")
    List<String> photoKeys = new ArrayList<>();
    @CreationTimestamp
    Instant createdAt;
    @UpdateTimestamp
    Instant updatedAt;
    String createdBy;
    @Version
    Long version;
}
