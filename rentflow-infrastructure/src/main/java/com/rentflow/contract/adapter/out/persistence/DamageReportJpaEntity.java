package com.rentflow.contract.adapter.out.persistence;

import com.rentflow.contract.DamageLiability;
import com.rentflow.contract.DamageReportStatus;
import com.rentflow.contract.DamageSeverity;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "damage_reports")
class DamageReportJpaEntity {
    @Id
    UUID id;
    @Column(nullable = false)
    UUID vehicleId;
    UUID contractId;
    UUID customerId;
    @Column(nullable = false)
    String damageDescription;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    DamageSeverity severity;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    DamageReportStatus status;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    DamageLiability liability;
    @Column(nullable = false)
    BigDecimal estimatedCost;
    BigDecimal actualCost;
    @Column(nullable = false, length = 3)
    String currency;
    @Column(nullable = false)
    Instant reportedAt;
    @ElementCollection
    @CollectionTable(name = "damage_report_photos", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "photo_key")
    List<String> photoKeys = new ArrayList<>();
    @CreationTimestamp
    Instant createdAt;
    @UpdateTimestamp
    Instant updatedAt;
    @Version
    Long version;
}
