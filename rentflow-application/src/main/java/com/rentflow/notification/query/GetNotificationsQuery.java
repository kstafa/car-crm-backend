package com.rentflow.notification.query;

import com.rentflow.shared.id.StaffId;
import jakarta.validation.constraints.NotNull;

public record GetNotificationsQuery(
        @NotNull StaffId staffId,
        Boolean unreadOnly,
        int page,
        int size) {
}
