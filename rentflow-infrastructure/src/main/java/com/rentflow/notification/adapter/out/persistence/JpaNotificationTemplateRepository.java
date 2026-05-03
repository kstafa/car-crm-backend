package com.rentflow.notification.adapter.out.persistence;

import com.rentflow.notification.NotificationTemplate;
import com.rentflow.notification.NotificationTemplateId;
import com.rentflow.notification.NotificationTrigger;
import com.rentflow.notification.port.out.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Primary
@RequiredArgsConstructor
class JpaNotificationTemplateRepository implements NotificationTemplateRepository {
    private final SpringDataNotificationTemplateRepo repo;
    private final NotificationJpaMapper mapper;

    @Override
    public void save(NotificationTemplate template) {
        NotificationTemplateJpaEntity entity = mapper.toJpa(template);
        repo.findById(entity.id).ifPresent(existing -> {
            entity.createdAt = existing.createdAt;
            entity.version = existing.version;
        });
        repo.save(entity);
    }

    @Override
    public Optional<NotificationTemplate> findById(NotificationTemplateId id) {
        return repo.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<NotificationTemplate> findActiveByTrigger(NotificationTrigger trigger) {
        return repo.findByTriggerAndActiveTrue(trigger).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<NotificationTemplate> findAll() {
        return repo.findAll().stream().map(mapper::toDomain).toList();
    }
}
