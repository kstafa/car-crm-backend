package com.rentflow.notification;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class NotificationTemplateTest {

    @Test
    void create_validParams_setsActiveTrue() {
        NotificationTemplate template = template();

        assertTrue(template.isActive());
        assertEquals(NotificationTrigger.RESERVATION_CONFIRMED, template.getTrigger());
        assertEquals(NotificationChannel.EMAIL, template.getChannel());
    }

    @Test
    void create_blankName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> NotificationTemplate.create(" ",
                NotificationTrigger.RESERVATION_CONFIRMED, NotificationChannel.EMAIL, "Subject", "Body"));
    }

    @Test
    void create_blankBody_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> NotificationTemplate.create("Reservation",
                NotificationTrigger.RESERVATION_CONFIRMED, NotificationChannel.EMAIL, "Subject", " "));
    }

    @Test
    void render_singlePlaceholder_replacesCorrectly() {
        NotificationTemplate template = NotificationTemplate.create("Reservation",
                NotificationTrigger.RESERVATION_CONFIRMED, NotificationChannel.EMAIL, "Subject",
                "Hello {{customerName}}");

        assertEquals("Hello Ada", template.render(Map.of("customerName", "Ada")));
    }

    @Test
    void render_multiplePlaceholders_replacesAll() {
        NotificationTemplate template = NotificationTemplate.create("Reservation",
                NotificationTrigger.RESERVATION_CONFIRMED, NotificationChannel.EMAIL, "Subject",
                "{{customerName}} booking {{reservationNumber}} confirmed");

        assertEquals("Ada booking R-1 confirmed", template.render(Map.of(
                "customerName", "Ada",
                "reservationNumber", "R-1")));
    }

    @Test
    void render_unknownPlaceholder_leavesAsIs() {
        NotificationTemplate template = NotificationTemplate.create("Reservation",
                NotificationTrigger.RESERVATION_CONFIRMED, NotificationChannel.EMAIL, "Subject",
                "Hello {{customerName}} {{unknown}}");

        assertEquals("Hello Ada {{unknown}}", template.render(Map.of("customerName", "Ada")));
    }

    @Test
    void render_emptyVariables_returnsBodyUnchanged() {
        NotificationTemplate template = template();

        assertEquals("Body {{name}}", template.render(Map.of()));
    }

    @Test
    void renderSubject_nullSubject_returnsEmptyString() {
        NotificationTemplate template = NotificationTemplate.create("Reservation",
                NotificationTrigger.RESERVATION_CONFIRMED, NotificationChannel.EMAIL, null, "Body");

        assertEquals("", template.renderSubject(Map.of("name", "Ada")));
    }

    @Test
    void deactivate_activeThenDeactivate_isInactive() {
        NotificationTemplate template = template();

        template.deactivate();

        assertFalse(template.isActive());
    }

    @Test
    void updateTemplates_blankBody_throwsIllegalArgumentException() {
        NotificationTemplate template = template();

        assertThrows(IllegalArgumentException.class, () -> template.updateTemplates("New", " "));
    }

    private static NotificationTemplate template() {
        return NotificationTemplate.create("Reservation", NotificationTrigger.RESERVATION_CONFIRMED,
                NotificationChannel.EMAIL, "Subject {{name}}", "Body {{name}}");
    }
}
