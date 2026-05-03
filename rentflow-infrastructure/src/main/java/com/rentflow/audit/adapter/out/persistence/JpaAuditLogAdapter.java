package com.rentflow.audit.adapter.out.persistence;

import com.rentflow.shared.AuditEntry;
import com.rentflow.shared.port.out.AuditLogPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Primary
@RequiredArgsConstructor
public class JpaAuditLogAdapter implements AuditLogPort {

    private final SpringDataAuditLogRepo repo;

    @Override
    @Async
    public void log(AuditEntry entry) {
        AuditLogJpaEntity entity = new AuditLogJpaEntity();
        entity.id = UUID.randomUUID();
        entity.actionType = entry.actionType();
        entity.entityType = entry.entityType();
        entity.entityId = entry.entityId();
        entity.actor = entry.actor() != null ? entry.actor() : "SYSTEM";
        entity.occurredAt = entry.occurredAt();
        entity.beforeValue = entry.beforeValue();
        entity.afterValue = entry.afterValue();
        entity.ipAddress = entry.ipAddress();
        repo.save(entity);
    }
}
