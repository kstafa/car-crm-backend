package com.rentflow.notification.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "staff_notifications")
class StaffNotificationJpaEntity {
    @Id
    UUID id;
    @Column(nullable = false)
    UUID recipientId;
    @Column(nullable = false)
    String title;
    @Column(columnDefinition = "text")
    String message;
    String entityType;
    String entityId;
    boolean read;
    @CreationTimestamp
    Instant createdAt;
}
