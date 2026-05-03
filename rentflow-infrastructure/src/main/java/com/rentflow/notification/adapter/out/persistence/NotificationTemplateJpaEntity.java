package com.rentflow.notification.adapter.out.persistence;

import com.rentflow.notification.NotificationChannel;
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
@Table(name = "notification_templates")
class NotificationTemplateJpaEntity {
    @Id
    UUID id;
    @Column(nullable = false, length = 100)
    String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger", nullable = false, length = 40)
    NotificationTrigger trigger;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    NotificationChannel channel;
    @Column(columnDefinition = "text")
    String subjectTemplate;
    @Column(nullable = false, columnDefinition = "text")
    String bodyTemplate;
    boolean active;
    @CreationTimestamp
    Instant createdAt;
    @UpdateTimestamp
    Instant updatedAt;
    @Version
    Long version;
}
