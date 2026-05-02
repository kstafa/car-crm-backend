package com.rentflow.reservation.adapter.out.persistence;

import com.rentflow.reservation.ReservationStatus;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reservations")
class ReservationJpaEntity {
    @Id
    UUID id;
    @Column(unique = true, nullable = false, length = 20)
    String reservationNumber;
    @Column(nullable = false)
    UUID customerId;
    @Column(nullable = false)
    UUID vehicleId;
    @Column(nullable = false)
    ZonedDateTime pickupDatetime;
    @Column(nullable = false)
    ZonedDateTime returnDatetime;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    ReservationStatus status;
    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal baseAmount;
    @Column(nullable = false, length = 3)
    String currency;
    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal discountAmount;
    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal depositAmount;
    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal taxAmount;
    @Column(columnDefinition = "text")
    String notes;
    String createdBy;
    @CreationTimestamp
    Instant createdAt;
    @UpdateTimestamp
    Instant updatedAt;
    @Version
    Long version;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "reservation_id")
    List<ReservationExtraJpaEntity> extras = new ArrayList<>();
}

@Entity
@Table(name = "reservation_extras")
class ReservationExtraJpaEntity {
    @Id
    UUID id;
    @Column(name = "reservation_id", insertable = false, updatable = false)
    UUID reservationId;
    @Column(length = 255)
    String name;
    @Column(precision = 10, scale = 2)
    BigDecimal unitPrice;
    int quantity;
}
