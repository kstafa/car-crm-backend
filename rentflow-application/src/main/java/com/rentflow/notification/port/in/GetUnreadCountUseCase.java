package com.rentflow.notification.port.in;

import com.rentflow.shared.id.StaffId;

public interface GetUnreadCountUseCase {
    int getUnreadCount(StaffId staffId);
}
