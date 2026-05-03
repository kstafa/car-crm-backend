package com.rentflow.notification.port.in;

import com.rentflow.notification.NotificationId;
import com.rentflow.shared.id.StaffId;

public interface MarkNotificationReadUseCase {
    void markRead(NotificationId id, StaffId staffId);
}
