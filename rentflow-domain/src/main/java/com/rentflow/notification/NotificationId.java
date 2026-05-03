package com.rentflow.notification;

import java.util.Objects;
import java.util.UUID;

public record NotificationId(UUID value) {
    public NotificationId {
        Objects.requireNonNull(value);
    }

    public static NotificationId generate() {
        return new NotificationId(UUID.randomUUID());
    }

    public static NotificationId of(UUID value) {
        return new NotificationId(value);
    }

    public static NotificationId of(String value) {
        return new NotificationId(UUID.fromString(value));
    }
}
