package com.rentflow.notification;

import java.util.Objects;
import java.util.UUID;

public record AutomationRuleId(UUID value) {
    public AutomationRuleId {
        Objects.requireNonNull(value);
    }

    public static AutomationRuleId generate() {
        return new AutomationRuleId(UUID.randomUUID());
    }

    public static AutomationRuleId of(UUID value) {
        return new AutomationRuleId(value);
    }

    public static AutomationRuleId of(String value) {
        return new AutomationRuleId(UUID.fromString(value));
    }
}
