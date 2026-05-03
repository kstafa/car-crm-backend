package com.rentflow.notification.service;

import com.rentflow.notification.AutomationRule;
import com.rentflow.notification.AutomationRuleId;
import com.rentflow.notification.NotificationChannel;
import com.rentflow.notification.NotificationId;
import com.rentflow.notification.NotificationTemplate;
import com.rentflow.notification.NotificationTemplateId;
import com.rentflow.notification.NotificationTrigger;
import com.rentflow.notification.StaffNotification;
import com.rentflow.notification.command.CreateTemplateCommand;
import com.rentflow.notification.command.SendNotificationCommand;
import com.rentflow.notification.command.UpdateTemplateCommand;
import com.rentflow.notification.port.out.AutomationRuleRepository;
import com.rentflow.notification.port.out.NotificationTemplateRepository;
import com.rentflow.notification.port.out.StaffNotificationRepository;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.port.out.AuditLogPort;
import com.rentflow.shared.port.out.EmailSenderPort;
import com.rentflow.shared.port.out.SmsSenderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationApplicationServiceTest {

    @Mock NotificationTemplateRepository templateRepository;
    @Mock AutomationRuleRepository ruleRepository;
    @Mock StaffNotificationRepository notificationRepository;
    @Mock EmailSenderPort emailSender;
    @Mock SmsSenderPort smsSender;
    @Mock AuditLogPort auditLog;
    NotificationApplicationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationApplicationService(templateRepository, ruleRepository, notificationRepository,
                emailSender, smsSender, auditLog);
    }

    @Test
    void send_activeRuleAndTemplate_sendsEmailViaEmailSender() {
        AutomationRule rule = rule();
        NotificationTemplate template = template(NotificationChannel.EMAIL);
        when(ruleRepository.findActiveByTrigger(NotificationTrigger.RESERVATION_CONFIRMED)).thenReturn(List.of(rule));
        when(templateRepository.findById(rule.getTemplateId())).thenReturn(Optional.of(template));

        service.send(command());

        verify(emailSender).sendAsync("ada@example.com", "Subject R-1", "Body Ada");
    }

    @Test
    void send_emailChannel_callsSendAsync() {
        send_activeRuleAndTemplate_sendsEmailViaEmailSender();
    }

    @Test
    void send_inAppChannel_savesStaffNotification() {
        StaffId staffId = StaffId.generate();
        AutomationRule rule = rule();
        NotificationTemplate template = template(NotificationChannel.IN_APP);
        when(ruleRepository.findActiveByTrigger(NotificationTrigger.RESERVATION_CONFIRMED)).thenReturn(List.of(rule));
        when(templateRepository.findById(rule.getTemplateId())).thenReturn(Optional.of(template));

        service.send(new SendNotificationCommand(NotificationTrigger.RESERVATION_CONFIRMED,
                Map.of("customerName", "Ada", "reservationNumber", "R-1"),
                null, null, staffId, "Reservation", "R-1"));

        verify(notificationRepository).save(any(StaffNotification.class));
    }

    @Test
    void send_noActiveRulesForTrigger_sendsNothing() {
        when(ruleRepository.findActiveByTrigger(NotificationTrigger.RESERVATION_CONFIRMED)).thenReturn(List.of());

        service.send(command());

        verify(emailSender, never()).sendAsync(any(), any(), any());
    }

    @Test
    void send_templateInactive_skipsTemplate() {
        AutomationRule rule = rule();
        NotificationTemplate template = template(NotificationChannel.EMAIL);
        template.deactivate();
        when(ruleRepository.findActiveByTrigger(NotificationTrigger.RESERVATION_CONFIRMED)).thenReturn(List.of(rule));
        when(templateRepository.findById(rule.getTemplateId())).thenReturn(Optional.of(template));

        service.send(command());

        verify(emailSender, never()).sendAsync(any(), any(), any());
    }

    @Test
    void send_templateNotFound_skipsGracefully() {
        AutomationRule rule = rule();
        when(ruleRepository.findActiveByTrigger(NotificationTrigger.RESERVATION_CONFIRMED)).thenReturn(List.of(rule));
        when(templateRepository.findById(rule.getTemplateId())).thenReturn(Optional.empty());

        service.send(command());

        verify(emailSender, never()).sendAsync(any(), any(), any());
    }

    @Test
    void markRead_ownNotification_setsRead() {
        StaffId staffId = StaffId.generate();
        StaffNotification notification = StaffNotification.create(staffId, "Title", "Message", "Reservation", "1");
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        service.markRead(notification.getId(), staffId);

        assertTrue(notification.isRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markRead_foreignNotification_throwsAccessDeniedException() {
        StaffNotification notification = StaffNotification.create(StaffId.generate(), "Title", "Message",
                "Reservation", "1");
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));

        assertThrows(AccessDeniedException.class, () -> service.markRead(notification.getId(), StaffId.generate()));
    }

    @Test
    void getUnreadCount_delegatesToRepository() {
        StaffId staffId = StaffId.generate();
        when(notificationRepository.countUnread(staffId)).thenReturn(3);

        service.getUnreadCount(staffId);

        verify(notificationRepository).countUnread(staffId);
    }

    @Test
    void markAllRead_delegatesToRepository() {
        StaffId staffId = StaffId.generate();

        service.markAllRead(staffId);

        verify(notificationRepository).markAllRead(staffId);
    }

    @Test
    void createTemplate_validCommand_savesAndReturnsId() {
        var id = service.create(new CreateTemplateCommand("Name", NotificationTrigger.INVOICE_SENT,
                NotificationChannel.EMAIL, "Subject", "Body", StaffId.generate()));

        verify(templateRepository).save(any(NotificationTemplate.class));
        assertTrue(id.value().version() >= 0);
    }

    @Test
    void updateTemplate_validCommand_updatesAndSaves() {
        NotificationTemplate template = template(NotificationChannel.EMAIL);
        when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));

        service.update(new UpdateTemplateCommand(template.getId(), "New", "New body", StaffId.generate()));

        verify(templateRepository).save(template);
    }

    @Test
    void toggleRule_activeRule_becomesInactive() {
        AutomationRule rule = rule();
        when(ruleRepository.findById(rule.getId())).thenReturn(Optional.of(rule));

        service.toggle(rule.getId());

        assertFalse(rule.isActive());
        verify(ruleRepository).save(rule);
    }

    @Test
    void toggleRule_inactiveRule_becomesActive() {
        AutomationRule rule = rule();
        rule.toggle();
        when(ruleRepository.findById(rule.getId())).thenReturn(Optional.of(rule));

        service.toggle(rule.getId());

        assertTrue(rule.isActive());
    }

    private static AutomationRule rule() {
        return AutomationRule.create("Rule", NotificationTrigger.RESERVATION_CONFIRMED,
                NotificationTemplateId.generate(), 0);
    }

    private static NotificationTemplate template(NotificationChannel channel) {
        return NotificationTemplate.create("Template", NotificationTrigger.RESERVATION_CONFIRMED, channel,
                "Subject {{reservationNumber}}", "Body {{customerName}}");
    }

    private static SendNotificationCommand command() {
        return new SendNotificationCommand(NotificationTrigger.RESERVATION_CONFIRMED,
                Map.of("customerName", "Ada", "reservationNumber", "R-1"),
                "ada@example.com", null, null, "Reservation", "R-1");
    }
}
