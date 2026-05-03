package com.rentflow.notification.port.in;

import com.rentflow.notification.model.StaffNotificationSummary;
import com.rentflow.notification.query.GetNotificationsQuery;
import org.springframework.data.domain.Page;

public interface GetNotificationsUseCase {
    Page<StaffNotificationSummary> get(GetNotificationsQuery query);
}
