package com.rentflow.notification.adapter.out.persistence;

import com.rentflow.notification.NotificationTrigger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataNotificationTemplateRepo extends JpaRepository<NotificationTemplateJpaEntity, UUID> {
    List<NotificationTemplateJpaEntity> findByTriggerAndActiveTrue(NotificationTrigger trigger);
}
