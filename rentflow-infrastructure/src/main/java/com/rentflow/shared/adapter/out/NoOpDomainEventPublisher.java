package com.rentflow.shared.adapter.out;

import com.rentflow.shared.DomainEvent;
import com.rentflow.shared.port.out.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class NoOpDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoOpDomainEventPublisher.class);

    @Override
    public void publish(DomainEvent event) {
        log.info("Domain event published: {} at {}", event.getClass().getSimpleName(), event.occurredAt());
    }
}
