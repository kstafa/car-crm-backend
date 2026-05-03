package com.rentflow.notification.adapter.out.persistence;

import com.rentflow.notification.AutomationRule;
import com.rentflow.notification.AutomationRuleId;
import com.rentflow.notification.NotificationTrigger;
import com.rentflow.notification.port.out.AutomationRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Primary
@RequiredArgsConstructor
class JpaAutomationRuleRepository implements AutomationRuleRepository {
    private final SpringDataAutomationRuleRepo repo;
    private final NotificationJpaMapper mapper;

    @Override
    public void save(AutomationRule rule) {
        AutomationRuleJpaEntity entity = mapper.toJpa(rule);
        repo.findById(entity.id).ifPresent(existing -> {
            entity.createdAt = existing.createdAt;
            entity.version = existing.version;
        });
        repo.save(entity);
    }

    @Override
    public Optional<AutomationRule> findById(AutomationRuleId id) {
        return repo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<AutomationRule> findActiveByTrigger(NotificationTrigger trigger) {
        return repo.findByTriggerAndActiveTrue(trigger).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<AutomationRule> findAll() {
        return repo.findAll().stream().map(mapper::toDomain).toList();
    }
}
