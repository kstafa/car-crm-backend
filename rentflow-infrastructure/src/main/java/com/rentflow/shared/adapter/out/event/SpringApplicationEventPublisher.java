package com.rentflow.shared.adapter.out.event;

import com.rentflow.shared.DomainEvent;
import com.rentflow.shared.port.out.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class SpringApplicationEventPublisher implements DomainEventPublisher {
    private final ApplicationEventPublisher springPublisher;

    @Override
    public void publish(DomainEvent event) {
        springPublisher.publishEvent(event);
    }
}
