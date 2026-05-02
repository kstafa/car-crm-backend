package com.rentflow.contract.adapter.out.persistence;

import com.rentflow.contract.ContractStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "contracts")
class ContractJpaEntity {
    @Id
    UUID id;
    @Column(unique = true, nullable = false)
    String contractNumber;
    @Column(nullable = false)
    UUID reservationId;
    @Column(nullable = false)
    UUID customerId;
    @Column(nullable = false)
    UUID vehicleId;
    @Column(nullable = false)
    ZonedDateTime scheduledPickup;
    @Column(nullable = false)
    ZonedDateTime scheduledReturn;
    ZonedDateTime actualPickupDatetime;
    ZonedDateTime actualReturnDatetime;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ContractStatus status;
    String signatureKey;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "contract_id", insertable = false, updatable = false)
    List<InspectionJpaEntity> inspections = new ArrayList<>();
    @CreationTimestamp
    Instant createdAt;
    @UpdateTimestamp
    Instant updatedAt;
    String createdBy;
    @Version
    Long version;
}
