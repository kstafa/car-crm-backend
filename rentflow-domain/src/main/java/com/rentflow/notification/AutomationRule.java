package com.rentflow.notification;

import com.rentflow.shared.AggregateRoot;

import java.util.Objects;

public class AutomationRule extends AggregateRoot {

    private final AutomationRuleId id;
    private String name;
    private NotificationTrigger trigger;
    private NotificationTemplateId templateId;
    private boolean active;
    private int delayMinutes;

    private AutomationRule(AutomationRuleId id, String name, NotificationTrigger trigger,
                           NotificationTemplateId templateId, boolean active, int delayMinutes) {
        this.id = Objects.requireNonNull(id);
        this.name = name;
        this.trigger = trigger;
        this.templateId = templateId;
        this.active = active;
        this.delayMinutes = delayMinutes;
    }

    public static AutomationRule create(String name, NotificationTrigger trigger, NotificationTemplateId templateId,
                                        int delayMinutes) {
        validate(name, trigger, templateId, delayMinutes);
        return new AutomationRule(AutomationRuleId.generate(), name, trigger, templateId, true, delayMinutes);
    }

    public static AutomationRule reconstitute(AutomationRuleId id, String name, NotificationTrigger trigger,
                                              NotificationTemplateId templateId, boolean active, int delayMinutes) {
        validate(name, trigger, templateId, delayMinutes);
        return new AutomationRule(id, name, trigger, templateId, active, delayMinutes);
    }

    public void toggle() {
        active = !active;
    }

    public AutomationRuleId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public NotificationTrigger getTrigger() {
        return trigger;
    }

    public NotificationTemplateId getTemplateId() {
        return templateId;
    }

    public boolean isActive() {
        return active;
    }

    public int getDelayMinutes() {
        return delayMinutes;
    }

    private static void validate(String name, NotificationTrigger trigger, NotificationTemplateId templateId,
                                 int delayMinutes) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(trigger);
        Objects.requireNonNull(templateId);
        if (delayMinutes < 0) {
            throw new IllegalArgumentException("delayMinutes must be >= 0");
        }
    }
}
