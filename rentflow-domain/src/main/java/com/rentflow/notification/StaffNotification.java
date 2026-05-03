package com.rentflow.notification;

import com.rentflow.shared.AggregateRoot;
import com.rentflow.shared.id.StaffId;

import java.time.Instant;
import java.util.Objects;

public class StaffNotification extends AggregateRoot {

    private final NotificationId id;
    private StaffId recipientId;
    private String title;
    private String message;
    private String entityType;
    private String entityId;
    private boolean read;
    private Instant createdAt;

    private StaffNotification(NotificationId id, StaffId recipientId, String title, String message,
                              String entityType, String entityId, boolean read, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.recipientId = recipientId;
        this.title = title;
        this.message = message;
        this.entityType = entityType;
        this.entityId = entityId;
        this.read = read;
        this.createdAt = createdAt;
    }

    public static StaffNotification create(StaffId recipientId, String title, String message,
                                           String entityType, String entityId) {
        Objects.requireNonNull(recipientId);
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        return new StaffNotification(NotificationId.generate(), recipientId, title, message, entityType, entityId,
                false, Instant.now());
    }

    public static StaffNotification reconstitute(NotificationId id, StaffId recipientId, String title, String message,
                                                 String entityType, String entityId, boolean read, Instant createdAt) {
        return new StaffNotification(id, recipientId, title, message, entityType, entityId, read, createdAt);
    }

    public void markRead() {
        read = true;
    }

    public NotificationId getId() {
        return id;
    }

    public StaffId getRecipientId() {
        return recipientId;
    }

    public boolean isRead() {
        return read;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
