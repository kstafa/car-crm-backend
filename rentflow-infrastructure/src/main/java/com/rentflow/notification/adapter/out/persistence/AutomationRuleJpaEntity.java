package com.rentflow.notification.adapter.out.persistence;

import com.rentflow.notification.NotificationTrigger;
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
@Table(name = "automation_rules")
class AutomationRuleJpaEntity {
    @Id
    UUID id;
    @Column(nullable = false, length = 100)
    String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger", nullable = false, length = 40)
    NotificationTrigger trigger;
    @Column(nullable = false)
    UUID templateId;
    boolean active;
    int delayMinutes;
    @CreationTimestamp
    Instant createdAt;
    @UpdateTimestamp
    Instant updatedAt;
    @Version
    Long version;
}
