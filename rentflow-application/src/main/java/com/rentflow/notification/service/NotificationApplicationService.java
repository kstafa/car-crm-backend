package com.rentflow.notification.service;

import com.rentflow.notification.AutomationRule;
import com.rentflow.notification.AutomationRuleId;
import com.rentflow.notification.NotificationChannel;
import com.rentflow.notification.NotificationId;
import com.rentflow.notification.NotificationTemplate;
import com.rentflow.notification.NotificationTemplateId;
import com.rentflow.notification.StaffNotification;
import com.rentflow.notification.command.CreateAutomationRuleCommand;
import com.rentflow.notification.command.CreateTemplateCommand;
import com.rentflow.notification.command.SendNotificationCommand;
import com.rentflow.notification.command.UpdateTemplateCommand;
import com.rentflow.notification.model.AutomationRuleSummary;
import com.rentflow.notification.model.NotificationTemplateSummary;
import com.rentflow.notification.model.StaffNotificationSummary;
import com.rentflow.notification.port.in.CreateAutomationRuleUseCase;
import com.rentflow.notification.port.in.CreateTemplateUseCase;
import com.rentflow.notification.port.in.GetNotificationsUseCase;
import com.rentflow.notification.port.in.GetUnreadCountUseCase;
import com.rentflow.notification.port.in.MarkAllNotificationsReadUseCase;
import com.rentflow.notification.port.in.MarkNotificationReadUseCase;
import com.rentflow.notification.port.in.SendNotificationUseCase;
import com.rentflow.notification.port.in.ToggleAutomationRuleUseCase;
import com.rentflow.notification.port.in.UpdateTemplateUseCase;
import com.rentflow.notification.port.out.AutomationRuleRepository;
import com.rentflow.notification.port.out.NotificationTemplateRepository;
import com.rentflow.notification.port.out.StaffNotificationRepository;
import com.rentflow.notification.query.GetNotificationsQuery;
import com.rentflow.shared.AuditEntry;
import com.rentflow.shared.ResourceNotFoundException;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.port.out.AuditLogPort;
import com.rentflow.shared.port.out.EmailSenderPort;
import com.rentflow.shared.port.out.SmsSenderPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificationApplicationService implements SendNotificationUseCase, GetNotificationsUseCase,
        MarkNotificationReadUseCase, MarkAllNotificationsReadUseCase, GetUnreadCountUseCase, CreateTemplateUseCase,
        UpdateTemplateUseCase, CreateAutomationRuleUseCase, ToggleAutomationRuleUseCase {

    private final NotificationTemplateRepository templateRepository;
    private final AutomationRuleRepository ruleRepository;
    private final StaffNotificationRepository notificationRepository;
    private final EmailSenderPort emailSender;
    private final SmsSenderPort smsSender;
    private final AuditLogPort auditLog;

    public NotificationApplicationService(NotificationTemplateRepository templateRepository,
                                          AutomationRuleRepository ruleRepository,
                                          StaffNotificationRepository notificationRepository,
                                          EmailSenderPort emailSender,
                                          SmsSenderPort smsSender,
                                          AuditLogPort auditLog) {
        this.templateRepository = templateRepository;
        this.ruleRepository = ruleRepository;
        this.notificationRepository = notificationRepository;
        this.emailSender = emailSender;
        this.smsSender = smsSender;
        this.auditLog = auditLog;
    }

    @Override
    public void send(SendNotificationCommand command) {
        List<AutomationRule> rules = ruleRepository.findActiveByTrigger(command.trigger());
        for (AutomationRule rule : rules) {
            NotificationTemplate template = templateRepository.findById(rule.getTemplateId()).orElse(null);
            if (template == null || !template.isActive()) {
                continue;
            }

            String renderedBody = template.render(command.variables());
            String renderedSubject = template.renderSubject(command.variables());

            if (template.getChannel() == NotificationChannel.EMAIL && command.recipientEmail() != null) {
                emailSender.sendAsync(command.recipientEmail(), renderedSubject, renderedBody);
            } else if (template.getChannel() == NotificationChannel.SMS && command.recipientPhone() != null) {
                smsSender.send(command.recipientPhone(), renderedBody);
            } else if (template.getChannel() == NotificationChannel.IN_APP && command.recipientStaffId() != null) {
                StaffNotification notification = StaffNotification.create(
                        command.recipientStaffId(),
                        renderedSubject.isBlank() ? command.trigger().name() : renderedSubject,
                        renderedBody,
                        command.entityType(),
                        command.entityId());
                notificationRepository.save(notification);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StaffNotificationSummary> get(GetNotificationsQuery query) {
        int size = query.size() <= 0 ? 20 : query.size();
        int page = Math.max(query.page(), 0);
        return notificationRepository.findByRecipient(query.staffId(), PageRequest.of(page, size));
    }

    @Override
    public void markRead(NotificationId id, StaffId staffId) {
        StaffNotification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id.value()));
        if (!notification.getRecipientId().equals(staffId)) {
            throw new AccessDeniedException("Notification does not belong to staff member");
        }
        notification.markRead();
        notificationRepository.save(notification);
    }

    @Override
    public void markAllRead(StaffId staffId) {
        notificationRepository.markAllRead(staffId);
    }

    @Override
    @Transactional(readOnly = true)
    public int getUnreadCount(StaffId staffId) {
        return notificationRepository.countUnread(staffId);
    }

    @Override
    public NotificationTemplateId create(CreateTemplateCommand command) {
        NotificationTemplate template = NotificationTemplate.create(command.name(), command.trigger(),
                command.channel(), command.subjectTemplate(), command.bodyTemplate());
        templateRepository.save(template);
        auditLog.log(AuditEntry.of("NOTIFICATION_TEMPLATE_CREATED", template.getId(), command.createdBy()));
        return template.getId();
    }

    @Override
    public void update(UpdateTemplateCommand command) {
        NotificationTemplate template = templateRepository.findById(command.templateId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification template not found: " + command.templateId().value()));
        template.updateTemplates(command.newSubject(), command.newBody());
        templateRepository.save(template);
        auditLog.log(AuditEntry.of("NOTIFICATION_TEMPLATE_UPDATED", template.getId(), command.updatedBy()));
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplateSummary> listTemplates() {
        return templateRepository.findAll().stream()
                .map(template -> new NotificationTemplateSummary(template.getId(), template.getName(),
                        template.getTrigger(), template.getChannel(), template.isActive()))
                .toList();
    }

    @Override
    public AutomationRuleId create(CreateAutomationRuleCommand command) {
        templateRepository.findById(command.templateId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification template not found: " + command.templateId().value()));
        AutomationRule rule = AutomationRule.create(command.name(), command.trigger(), command.templateId(),
                command.delayMinutes());
        ruleRepository.save(rule);
        auditLog.log(AuditEntry.of("AUTOMATION_RULE_CREATED", rule.getId(), command.createdBy()));
        return rule.getId();
    }

    @Override
    public void toggle(AutomationRuleId id) {
        AutomationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Automation rule not found: " + id.value()));
        rule.toggle();
        ruleRepository.save(rule);
        auditLog.log(AuditEntry.of("AUTOMATION_RULE_TOGGLED", rule.getId(), null));
    }

    @Transactional(readOnly = true)
    public List<AutomationRuleSummary> listAutomationRules() {
        return ruleRepository.findAll().stream()
                .map(rule -> new AutomationRuleSummary(rule.getId(), rule.getName(), rule.getTrigger(),
                        rule.getTemplateId(), rule.isActive(), rule.getDelayMinutes()))
                .toList();
    }
}
