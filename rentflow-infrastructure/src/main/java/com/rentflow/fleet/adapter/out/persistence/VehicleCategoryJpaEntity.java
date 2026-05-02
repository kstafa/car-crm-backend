package com.rentflow.fleet.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vehicle_categories")
class VehicleCategoryJpaEntity {
    @Id
    UUID id;
    @Column(unique = true, nullable = false, length = 100)
    String name;
    @Column(columnDefinition = "text")
    String description;
    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal baseDailyRate;
    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal depositAmount;
    @Column(nullable = false, length = 3)
    String currency;
    @Column(nullable = false, precision = 4, scale = 2)
    BigDecimal taxRate;
    boolean active;
    @CreationTimestamp
    Instant createdAt;
    @UpdateTimestamp
    Instant updatedAt;
    @Version
    Long version;
}
