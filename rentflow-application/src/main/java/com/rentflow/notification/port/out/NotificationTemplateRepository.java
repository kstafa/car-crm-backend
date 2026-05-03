package com.rentflow.notification.port.out;

import com.rentflow.notification.NotificationTemplate;
import com.rentflow.notification.NotificationTemplateId;
import com.rentflow.notification.NotificationTrigger;

import java.util.List;
import java.util.Optional;

public interface NotificationTemplateRepository {
    void save(NotificationTemplate template);

    Optional<NotificationTemplate> findById(NotificationTemplateId id);

    List<NotificationTemplate> findActiveByTrigger(NotificationTrigger trigger);

    List<NotificationTemplate> findAll();
}
