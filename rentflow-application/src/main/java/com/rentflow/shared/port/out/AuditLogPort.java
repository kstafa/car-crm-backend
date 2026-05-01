package com.rentflow.shared.port.out;

import com.rentflow.shared.AuditEntry;

public interface AuditLogPort {
    void log(AuditEntry entry);
}
