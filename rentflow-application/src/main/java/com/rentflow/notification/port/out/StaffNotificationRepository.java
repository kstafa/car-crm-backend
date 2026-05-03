package com.rentflow.notification.port.out;

import com.rentflow.notification.NotificationId;
import com.rentflow.notification.StaffNotification;
import com.rentflow.notification.model.StaffNotificationSummary;
import com.rentflow.shared.id.StaffId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface StaffNotificationRepository {
    void save(StaffNotification notification);

    void saveAll(List<StaffNotification> notifications);

    Optional<StaffNotification> findById(NotificationId id);

    Page<StaffNotificationSummary> findByRecipient(StaffId staffId, Pageable pageable);

    int countUnread(StaffId staffId);

    void markAllRead(StaffId staffId);
}
