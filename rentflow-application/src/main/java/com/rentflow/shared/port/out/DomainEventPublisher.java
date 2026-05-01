package com.rentflow.shared.port.out;

import com.rentflow.shared.DomainEvent;

public interface DomainEventPublisher {
    void publish(DomainEvent event);
}
