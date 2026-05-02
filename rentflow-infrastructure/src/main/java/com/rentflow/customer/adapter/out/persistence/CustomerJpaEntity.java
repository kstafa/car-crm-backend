package com.rentflow.customer.adapter.out.persistence;

import com.rentflow.customer.CustomerStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customers")
class CustomerJpaEntity {
    @Id
    UUID id;
    @Column(nullable = false, length = 100)
    String firstName;
    @Column(nullable = false, length = 100)
    String lastName;
    @Column(unique = true, nullable = false, length = 255)
    String email;
    @Column(length = 30)
    String phone;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    CustomerStatus status;
    @Column(columnDefinition = "text")
    String blacklistReason;
    @CreationTimestamp
    Instant createdAt;
    @UpdateTimestamp
    Instant updatedAt;
    @Version
    Long version;
}
