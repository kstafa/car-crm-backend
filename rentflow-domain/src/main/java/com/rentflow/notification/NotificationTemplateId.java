package com.rentflow.notification;

import java.util.Objects;
import java.util.UUID;

public record NotificationTemplateId(UUID value) {
    public NotificationTemplateId {
        Objects.requireNonNull(value);
    }

    public static NotificationTemplateId generate() {
        return new NotificationTemplateId(UUID.randomUUID());
    }

    public static NotificationTemplateId of(UUID value) {
        return new NotificationTemplateId(value);
    }

    public static NotificationTemplateId of(String value) {
        return new NotificationTemplateId(UUID.fromString(value));
    }
}
