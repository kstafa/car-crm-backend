package com.rentflow.notification.adapter.out.persistence;

import com.rentflow.notification.AutomationRule;
import com.rentflow.notification.AutomationRuleId;
import com.rentflow.notification.NotificationId;
import com.rentflow.notification.NotificationTemplate;
import com.rentflow.notification.NotificationTemplateId;
import com.rentflow.notification.StaffNotification;
import com.rentflow.notification.model.StaffNotificationSummary;
import com.rentflow.shared.id.StaffId;
import org.springframework.stereotype.Component;

@Component
class NotificationJpaMapper {

    NotificationTemplateJpaEntity toJpa(NotificationTemplate domain) {
        NotificationTemplateJpaEntity entity = new NotificationTemplateJpaEntity();
        entity.id = domain.getId().value();
        entity.name = domain.getName();
        entity.trigger = domain.getTrigger();
        entity.channel = domain.getChannel();
        entity.subjectTemplate = domain.getSubjectTemplate();
        entity.bodyTemplate = domain.getBodyTemplate();
        entity.active = domain.isActive();
        return entity;
    }

    NotificationTemplate toDomain(NotificationTemplateJpaEntity entity) {
        return NotificationTemplate.reconstitute(NotificationTemplateId.of(entity.id), entity.name, entity.trigger,
                entity.channel, entity.subjectTemplate, entity.bodyTemplate, entity.active);
    }

    AutomationRuleJpaEntity toJpa(AutomationRule domain) {
        AutomationRuleJpaEntity entity = new AutomationRuleJpaEntity();
        entity.id = domain.getId().value();
        entity.name = domain.getName();
        entity.trigger = domain.getTrigger();
        entity.templateId = domain.getTemplateId().value();
        entity.active = domain.isActive();
        entity.delayMinutes = domain.getDelayMinutes();
        return entity;
    }

    AutomationRule toDomain(AutomationRuleJpaEntity entity) {
        return AutomationRule.reconstitute(AutomationRuleId.of(entity.id), entity.name, entity.trigger,
                NotificationTemplateId.of(entity.templateId), entity.active, entity.delayMinutes);
    }

    StaffNotificationJpaEntity toJpa(StaffNotification domain) {
        StaffNotificationJpaEntity entity = new StaffNotificationJpaEntity();
        entity.id = domain.getId().value();
        entity.recipientId = domain.getRecipientId().value();
        entity.title = domain.getTitle();
        entity.message = domain.getMessage();
        entity.entityType = domain.getEntityType();
        entity.entityId = domain.getEntityId();
        entity.read = domain.isRead();
        entity.createdAt = domain.getCreatedAt();
        return entity;
    }

    StaffNotification toDomain(StaffNotificationJpaEntity entity) {
        return StaffNotification.reconstitute(NotificationId.of(entity.id), StaffId.of(entity.recipientId),
                entity.title, entity.message, entity.entityType, entity.entityId, entity.read, entity.createdAt);
    }

    StaffNotificationSummary toSummary(StaffNotificationJpaEntity entity) {
        return new StaffNotificationSummary(NotificationId.of(entity.id), entity.title, entity.message,
                entity.entityType, entity.entityId, entity.read, entity.createdAt);
    }
}
