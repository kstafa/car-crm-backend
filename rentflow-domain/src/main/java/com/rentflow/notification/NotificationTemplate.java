package com.rentflow.notification;

import com.rentflow.shared.AggregateRoot;

import java.util.Map;
import java.util.Objects;

public class NotificationTemplate extends AggregateRoot {

    private final NotificationTemplateId id;
    private String name;
    private NotificationTrigger trigger;
    private NotificationChannel channel;
    private String subjectTemplate;
    private String bodyTemplate;
    private boolean active;

    private NotificationTemplate(NotificationTemplateId id, String name, NotificationTrigger trigger,
                                 NotificationChannel channel, String subjectTemplate, String bodyTemplate,
                                 boolean active) {
        this.id = Objects.requireNonNull(id);
        this.name = name;
        this.trigger = trigger;
        this.channel = channel;
        this.subjectTemplate = subjectTemplate;
        this.bodyTemplate = bodyTemplate;
        this.active = active;
    }

    public static NotificationTemplate create(String name, NotificationTrigger trigger, NotificationChannel channel,
                                              String subjectTemplate, String bodyTemplate) {
        validateName(name);
        Objects.requireNonNull(trigger);
        Objects.requireNonNull(channel);
        validateBody(bodyTemplate);
        return new NotificationTemplate(NotificationTemplateId.generate(), name, trigger, channel,
                subjectTemplate, bodyTemplate, true);
    }

    public static NotificationTemplate reconstitute(NotificationTemplateId id, String name, NotificationTrigger trigger,
                                                   NotificationChannel channel, String subjectTemplate,
                                                   String bodyTemplate, boolean active) {
        return new NotificationTemplate(id, name, trigger, channel, subjectTemplate, bodyTemplate, active);
    }

    public void deactivate() {
        active = false;
    }

    public void activate() {
        active = true;
    }

    public void updateTemplates(String newSubject, String newBody) {
        validateBody(newBody);
        subjectTemplate = newSubject;
        bodyTemplate = newBody;
    }

    public String render(Map<String, String> variables) {
        return renderTemplate(bodyTemplate, variables);
    }

    public String renderSubject(Map<String, String> variables) {
        if (subjectTemplate == null) {
            return "";
        }
        return renderTemplate(subjectTemplate, variables);
    }

    public NotificationTemplateId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public NotificationTrigger getTrigger() {
        return trigger;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getBodyTemplate() {
        return bodyTemplate;
    }

    public String getSubjectTemplate() {
        return subjectTemplate;
    }

    public boolean isActive() {
        return active;
    }

    private static String renderTemplate(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

    private static void validateBody(String bodyTemplate) {
        if (bodyTemplate == null || bodyTemplate.isBlank()) {
            throw new IllegalArgumentException("bodyTemplate must not be blank");
        }
    }
}
