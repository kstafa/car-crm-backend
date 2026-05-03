package com.rentflow.notification.adapter.out.persistence;

import com.rentflow.notification.NotificationTrigger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataAutomationRuleRepo extends JpaRepository<AutomationRuleJpaEntity, UUID> {
    List<AutomationRuleJpaEntity> findByTriggerAndActiveTrue(NotificationTrigger trigger);
}
