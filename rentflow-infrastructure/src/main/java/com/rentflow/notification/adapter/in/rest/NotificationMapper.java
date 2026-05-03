package com.rentflow.notification.adapter.in.rest;

import com.rentflow.notification.NotificationChannel;
import com.rentflow.notification.NotificationTemplateId;
import com.rentflow.notification.NotificationTrigger;
import com.rentflow.notification.command.CreateAutomationRuleCommand;
import com.rentflow.notification.command.CreateTemplateCommand;
import com.rentflow.notification.command.UpdateTemplateCommand;
import com.rentflow.notification.model.AutomationRuleSummary;
import com.rentflow.notification.model.NotificationTemplateSummary;
import com.rentflow.notification.model.StaffNotificationSummary;
import com.rentflow.shared.adapter.in.rest.PageMeta;
import com.rentflow.shared.adapter.in.rest.PageResponse;
import com.rentflow.shared.id.StaffId;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NotificationMapper {

    PageResponse<StaffNotificationResponse> toPageResponse(Page<StaffNotificationSummary> page) {
        return new PageResponse<>(
                page.getContent().stream().map(this::toResponse).toList(),
                new PageMeta(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()));
    }

    StaffNotificationResponse toResponse(StaffNotificationSummary summary) {
        return new StaffNotificationResponse(summary.id().value(), summary.title(), summary.message(),
                summary.entityType(), summary.entityId(), summary.read(), summary.createdAt());
    }

    NotificationTemplateResponse toResponse(NotificationTemplateSummary summary) {
        return new NotificationTemplateResponse(summary.id().value(), summary.name(), summary.trigger().name(),
                summary.channel().name(), summary.active(), null, null);
    }

    AutomationRuleResponse toResponse(AutomationRuleSummary summary) {
        return new AutomationRuleResponse(summary.id().value(), summary.name(), summary.trigger().name(),
                summary.templateId().value(), summary.active(), summary.delayMinutes());
    }

    CreateTemplateCommand toCommand(CreateTemplateRequest request, StaffId staffId) {
        return new CreateTemplateCommand(request.name(), NotificationTrigger.valueOf(request.trigger()),
                NotificationChannel.valueOf(request.channel()), request.subjectTemplate(), request.bodyTemplate(),
                staffId);
    }

    UpdateTemplateCommand toCommand(UUID id, UpdateTemplateRequest request, StaffId staffId) {
        return new UpdateTemplateCommand(NotificationTemplateId.of(id), request.newSubject(), request.newBody(),
                staffId);
    }

    CreateAutomationRuleCommand toCommand(CreateAutomationRuleRequest request, StaffId staffId) {
        return new CreateAutomationRuleCommand(request.name(), NotificationTrigger.valueOf(request.trigger()),
                NotificationTemplateId.of(request.templateId()), request.delayMinutes(), staffId);
    }
}
