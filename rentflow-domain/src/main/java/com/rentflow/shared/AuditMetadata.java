package com.rentflow.shared;

import java.time.Instant;

public record AuditMetadata(Instant createdAt, Instant updatedAt, String createdBy, String lastModifiedBy) {
}
