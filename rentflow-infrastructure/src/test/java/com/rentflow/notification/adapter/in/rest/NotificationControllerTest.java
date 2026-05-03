package com.rentflow.notification.adapter.in.rest;

import com.rentflow.notification.AutomationRuleId;
import com.rentflow.notification.NotificationChannel;
import com.rentflow.notification.NotificationId;
import com.rentflow.notification.NotificationTemplateId;
import com.rentflow.notification.NotificationTrigger;
import com.rentflow.notification.command.CreateAutomationRuleCommand;
import com.rentflow.notification.command.CreateTemplateCommand;
import com.rentflow.notification.command.UpdateTemplateCommand;
import com.rentflow.notification.model.AutomationRuleSummary;
import com.rentflow.notification.model.NotificationTemplateSummary;
import com.rentflow.notification.model.StaffNotificationSummary;
import com.rentflow.notification.port.in.CreateAutomationRuleUseCase;
import com.rentflow.notification.port.in.CreateTemplateUseCase;
import com.rentflow.notification.port.in.GetNotificationsUseCase;
import com.rentflow.notification.port.in.GetUnreadCountUseCase;
import com.rentflow.notification.port.in.ListAutomationRulesUseCase;
import com.rentflow.notification.port.in.ListTemplatesUseCase;
import com.rentflow.notification.port.in.MarkAllNotificationsReadUseCase;
import com.rentflow.notification.port.in.MarkNotificationReadUseCase;
import com.rentflow.notification.port.in.ToggleAutomationRuleUseCase;
import com.rentflow.notification.port.in.UpdateTemplateUseCase;
import com.rentflow.security.JwtAuthFilter;
import com.rentflow.security.JwtTokenService;
import com.rentflow.security.SecurityConfig;
import com.rentflow.security.StaffPrincipal;
import com.rentflow.shared.adapter.in.GlobalExceptionHandler;
import com.rentflow.shared.adapter.in.rest.PageMeta;
import com.rentflow.shared.adapter.in.rest.PageResponse;
import com.rentflow.shared.id.StaffId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import({NotificationController.class, SecurityConfig.class, JwtAuthFilter.class, JwtTokenService.class,
        GlobalExceptionHandler.class})
@TestPropertySource(properties = "rentflow.jwt.secret=01234567890123456789012345678901")
class NotificationControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean GetNotificationsUseCase getNotifications;
    @MockBean MarkNotificationReadUseCase markRead;
    @MockBean MarkAllNotificationsReadUseCase markAllRead;
    @MockBean GetUnreadCountUseCase getUnreadCount;
    @MockBean ListTemplatesUseCase listTemplates;
    @MockBean CreateTemplateUseCase createTemplate;
    @MockBean UpdateTemplateUseCase updateTemplate;
    @MockBean ListAutomationRulesUseCase listRules;
    @MockBean CreateAutomationRuleUseCase createRule;
    @MockBean ToggleAutomationRuleUseCase toggleRule;
    @MockBean NotificationMapper mapper;

    @Test
    void listNotifications_authenticated_returns200WithPage() throws Exception {
        StaffNotificationSummary summary = notificationSummary();
        StaffNotificationResponse response = new StaffNotificationResponse(summary.id().value(), summary.title(),
                summary.message(), summary.entityType(), summary.entityId(), summary.read(), summary.createdAt());
        when(getNotifications.get(any())).thenReturn(new PageImpl<>(List.of(summary)));
        when(mapper.toPageResponse(any())).thenReturn(new PageResponse<>(List.of(response),
                new PageMeta(0, 1, 1, 1)));

        mockMvc.perform(get("/api/v1/notifications").with(user(principal("SETTINGS_VIEW"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Title"));
    }

    @Test
    void listNotifications_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
    }

    @Test
    void markRead_ownNotification_returns204() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/{id}/read", UUID.randomUUID())
                        .with(user(principal("SETTINGS_VIEW"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void markRead_foreignNotification_returns403() throws Exception {
        doThrow(new AccessDeniedException("Denied")).when(markRead).markRead(any(), any());

        mockMvc.perform(patch("/api/v1/notifications/{id}/read", UUID.randomUUID())
                        .with(user(principal("SETTINGS_VIEW"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void markAllRead_authenticated_returns204() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/read-all").with(user(principal("SETTINGS_VIEW"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void getUnreadCount_authenticated_returns200WithCount() throws Exception {
        when(getUnreadCount.getUnreadCount(any())).thenReturn(4);

        mockMvc.perform(get("/api/v1/notifications/unread-count").with(user(principal("SETTINGS_VIEW"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(4));
    }

    @Test
    @WithMockUser(authorities = "SETTINGS_VIEW")
    void listTemplates_settingsView_returns200() throws Exception {
        NotificationTemplateSummary summary = new NotificationTemplateSummary(NotificationTemplateId.generate(),
                "Template", NotificationTrigger.INVOICE_SENT, NotificationChannel.EMAIL, true);
        when(listTemplates.list()).thenReturn(List.of(summary));
        when(mapper.toResponse(summary)).thenReturn(new NotificationTemplateResponse(summary.id().value(),
                "Template", "INVOICE_SENT", "EMAIL", true, null, null));

        mockMvc.perform(get("/api/v1/notifications/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Template"));
    }

    @Test
    @WithMockUser(authorities = "REPORT_VIEW")
    void listTemplates_missingPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/templates")).andExpect(status().isForbidden());
    }

    @Test
    void createTemplate_settingsEdit_returns201() throws Exception {
        NotificationTemplateId id = NotificationTemplateId.generate();
        when(mapper.toCommand(any(CreateTemplateRequest.class), any())).thenReturn(new CreateTemplateCommand("Name",
                NotificationTrigger.INVOICE_SENT, NotificationChannel.EMAIL, "Subject", "Body", StaffId.generate()));
        when(createTemplate.create(any())).thenReturn(id);

        mockMvc.perform(post("/api/v1/notifications/templates")
                        .with(user(principal("SETTINGS_EDIT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(templateJson("Name")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.value().toString()));
    }

    @Test
    void createTemplate_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/templates")
                        .with(user(principal("SETTINGS_EDIT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(templateJson("")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateTemplate_settingsEdit_returns204() throws Exception {
        when(mapper.toCommand(any(UUID.class), any(UpdateTemplateRequest.class), any()))
                .thenReturn(new UpdateTemplateCommand(NotificationTemplateId.generate(), "Subject", "Body",
                        StaffId.generate()));

        mockMvc.perform(put("/api/v1/notifications/templates/{id}", UUID.randomUUID())
                        .with(user(principal("SETTINGS_EDIT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newSubject\":\"Subject\",\"newBody\":\"Body\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "SETTINGS_VIEW")
    void listRules_returns200() throws Exception {
        AutomationRuleSummary summary = new AutomationRuleSummary(AutomationRuleId.generate(), "Rule",
                NotificationTrigger.INVOICE_SENT, NotificationTemplateId.generate(), true, 0);
        when(listRules.list()).thenReturn(List.of(summary));
        when(mapper.toResponse(summary)).thenReturn(new AutomationRuleResponse(summary.id().value(), "Rule",
                "INVOICE_SENT", summary.templateId().value(), true, 0));

        mockMvc.perform(get("/api/v1/notifications/automation-rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Rule"));
    }

    @Test
    void createRule_validRequest_returns201() throws Exception {
        AutomationRuleId id = AutomationRuleId.generate();
        when(mapper.toCommand(any(CreateAutomationRuleRequest.class), any()))
                .thenReturn(new CreateAutomationRuleCommand("Rule", NotificationTrigger.INVOICE_SENT,
                        NotificationTemplateId.generate(), 0, StaffId.generate()));
        when(createRule.create(any())).thenReturn(id);

        mockMvc.perform(post("/api/v1/notifications/automation-rules")
                        .with(user(principal("SETTINGS_EDIT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Rule","trigger":"INVOICE_SENT","templateId":"%s","delayMinutes":0}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.value().toString()));
    }

    @Test
    void toggleRule_returns204() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/automation-rules/{id}/toggle", UUID.randomUUID())
                        .with(user(principal("SETTINGS_EDIT"))))
                .andExpect(status().isNoContent());
    }

    private static StaffPrincipal principal(String authority) {
        return new StaffPrincipal(StaffId.generate(), "staff@example.com", "ADMIN", Set.of(authority));
    }

    private static StaffNotificationSummary notificationSummary() {
        return new StaffNotificationSummary(NotificationId.generate(), "Title", "Message", "Reservation", "1",
                false, Instant.now());
    }

    private static String templateJson(String name) {
        return """
                {"name":"%s","trigger":"INVOICE_SENT","channel":"EMAIL","subjectTemplate":"Subject","bodyTemplate":"Body"}
                """.formatted(name);
    }
}
