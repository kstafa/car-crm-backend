package com.rentflow.staff.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "staff")
class StaffJpaEntity {
    @Id
    UUID id;
    @Column(unique = true, nullable = false, length = 255)
    String email;
    @Column(nullable = false)
    String passwordHash;
    @Column(nullable = false, length = 100)
    String firstName;
    @Column(nullable = false, length = 100)
    String lastName;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    RoleJpaEntity role;
    @Column(nullable = false, length = 20)
    String status;
    @CreationTimestamp
    Instant createdAt;
    @UpdateTimestamp
    Instant updatedAt;
    @Version
    Long version;
}
