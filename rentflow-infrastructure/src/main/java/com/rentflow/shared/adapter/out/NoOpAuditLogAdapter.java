package com.rentflow.shared.adapter.out;

import com.rentflow.shared.AuditEntry;
import com.rentflow.shared.port.out.AuditLogPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class NoOpAuditLogAdapter implements AuditLogPort {

    private static final Logger log = LoggerFactory.getLogger(NoOpAuditLogAdapter.class);

    @Override
    public void log(AuditEntry entry) {
        log.info("Audit entry: {}", entry);
    }
}
