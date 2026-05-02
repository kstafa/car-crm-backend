package com.rentflow.contract.adapter.out.persistence;

import com.rentflow.shared.FuelLevel;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "contract_inspections")
class InspectionJpaEntity {
    @Id
    UUID id;
    @Column(name = "contract_id", nullable = false)
    UUID contractId;
    @Column(length = 4, nullable = false)
    String type;
    boolean frontOk;
    boolean rearOk;
    boolean leftSideOk;
    boolean rightSideOk;
    boolean interiorOk;
    boolean trunkOk;
    boolean tiresOk;
    boolean lightsOk;
    String notes;
    @Enumerated(EnumType.STRING)
    FuelLevel fuelLevel;
    int mileage;
    Instant performedAt;
    UUID performedBy;
    @ElementCollection
    @CollectionTable(name = "inspection_photos", joinColumns = @JoinColumn(name = "inspection_id"))
    @Column(name = "photo_key")
    List<String> photoKeys = new ArrayList<>();
}
