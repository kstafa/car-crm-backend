package com.rentflow.notification.port.out;

import com.rentflow.notification.AutomationRule;
import com.rentflow.notification.AutomationRuleId;
import com.rentflow.notification.NotificationTrigger;

import java.util.List;
import java.util.Optional;

public interface AutomationRuleRepository {
    void save(AutomationRule rule);

    Optional<AutomationRule> findById(AutomationRuleId id);

    List<AutomationRule> findActiveByTrigger(NotificationTrigger trigger);

    List<AutomationRule> findAll();
}
