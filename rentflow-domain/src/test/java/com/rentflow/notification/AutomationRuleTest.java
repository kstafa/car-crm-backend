package com.rentflow.notification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutomationRuleTest {

    @Test
    void create_validParams_setsActiveTrue() {
        AutomationRule rule = rule(15);

        assertTrue(rule.isActive());
        assertEquals(NotificationTrigger.RESERVATION_CONFIRMED, rule.getTrigger());
    }

    @Test
    void create_blankName_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> AutomationRule.create(" ",
                NotificationTrigger.RESERVATION_CONFIRMED, NotificationTemplateId.generate(), 0));
    }

    @Test
    void create_negativeDelay_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> rule(-1));
    }

    @Test
    void create_zeroDelay_allowed() {
        AutomationRule rule = rule(0);

        assertEquals(0, rule.getDelayMinutes());
    }

    @Test
    void toggle_active_becomesInactive() {
        AutomationRule rule = rule(0);

        rule.toggle();

        assertFalse(rule.isActive());
    }

    @Test
    void toggle_inactive_becomesActive() {
        AutomationRule rule = rule(0);
        rule.toggle();

        rule.toggle();

        assertTrue(rule.isActive());
    }

    private static AutomationRule rule(int delayMinutes) {
        return AutomationRule.create("Send confirmation", NotificationTrigger.RESERVATION_CONFIRMED,
                NotificationTemplateId.generate(), delayMinutes);
    }
}
