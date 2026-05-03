package com.rentflow.notification;

import com.rentflow.shared.id.StaffId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaffNotificationTest {

    @Test
    void create_validParams_isUnreadOnCreation() {
        StaffNotification notification = notification();

        assertFalse(notification.isRead());
    }

    @Test
    void create_blankTitle_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> StaffNotification.create(
                StaffId.generate(), " ", "Message", "Reservation", "id"));
    }

    @Test
    void markRead_unreadNotification_becomesRead() {
        StaffNotification notification = notification();

        notification.markRead();

        assertTrue(notification.isRead());
    }

    @Test
    void markRead_alreadyRead_remainsRead() {
        StaffNotification notification = notification();
        notification.markRead();

        notification.markRead();

        assertTrue(notification.isRead());
    }

    private static StaffNotification notification() {
        return StaffNotification.create(StaffId.generate(), "Reservation confirmed", "Message",
                "Reservation", "id");
    }
}
