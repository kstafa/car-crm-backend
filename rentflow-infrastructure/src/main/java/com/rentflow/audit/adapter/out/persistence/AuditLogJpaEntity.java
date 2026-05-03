package com.rentflow.audit.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
class AuditLogJpaEntity {
    @Id
    UUID id;
    @Column(nullable = false)
    String actionType;
    String entityType;
    String entityId;
    @Column(nullable = false)
    String actor;
    @Column(nullable = false)
    Instant occurredAt;
    @Column(columnDefinition = "text")
    String beforeValue;
    @Column(columnDefinition = "text")
    String afterValue;
    String ipAddress;
}
