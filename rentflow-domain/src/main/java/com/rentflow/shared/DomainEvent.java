package com.rentflow.shared;

import java.time.Instant;

public interface DomainEvent {
    Instant occurredAt();
}
