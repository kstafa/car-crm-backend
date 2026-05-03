package com.rentflow.notification.port.in;

import com.rentflow.shared.id.StaffId;

public interface MarkAllNotificationsReadUseCase {
    void markAllRead(StaffId staffId);
}
