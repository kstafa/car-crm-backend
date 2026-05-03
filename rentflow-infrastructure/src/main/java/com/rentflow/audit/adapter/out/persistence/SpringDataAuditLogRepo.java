package com.rentflow.audit.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

public interface SpringDataAuditLogRepo extends JpaRepository<AuditLogJpaEntity, UUID>,
        JpaSpecificationExecutor<AuditLogJpaEntity> {

    default Page<AuditLogJpaEntity> findFiltered(
            String actionType,
            String actor,
            String entityType,
            Instant from,
            Instant to,
            Pageable pageable) {
        Specification<AuditLogJpaEntity> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (actionType != null) {
                predicates.add(cb.equal(root.get("actionType"), actionType));
            }
            if (actor != null) {
                predicates.add(cb.equal(root.get("actor"), actor));
            }
            if (entityType != null) {
                predicates.add(cb.equal(root.get("entityType"), entityType));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            }
            query.orderBy(cb.desc(root.get("occurredAt")));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return findAll(spec, pageable);
    }
}
