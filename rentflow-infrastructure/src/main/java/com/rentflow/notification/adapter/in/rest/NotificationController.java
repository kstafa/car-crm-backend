package com.rentflow.notification.adapter.in.rest;

import com.rentflow.notification.AutomationRuleId;
import com.rentflow.notification.NotificationId;
import com.rentflow.notification.NotificationTemplateId;
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
import com.rentflow.notification.query.GetNotificationsQuery;
import com.rentflow.security.StaffPrincipal;
import com.rentflow.shared.adapter.in.rest.PageResponse;
import com.rentflow.shared.id.StaffId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final GetNotificationsUseCase getNotifications;
    private final MarkNotificationReadUseCase markRead;
    private final MarkAllNotificationsReadUseCase markAllRead;
    private final GetUnreadCountUseCase getUnreadCount;
    private final ListTemplatesUseCase listTemplates;
    private final CreateTemplateUseCase createTemplate;
    private final UpdateTemplateUseCase updateTemplate;
    private final ListAutomationRulesUseCase listRules;
    private final CreateAutomationRuleUseCase createRule;
    private final ToggleAutomationRuleUseCase toggleRule;
    private final NotificationMapper mapper;

    @GetMapping
    public ResponseEntity<PageResponse<StaffNotificationResponse>> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Authentication authentication) {
        return ResponseEntity.ok(mapper.toPageResponse(getNotifications.get(
                new GetNotificationsQuery(staffId(authentication), null, page, size))));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable("id") UUID id, Authentication authentication) {
        markRead.markRead(NotificationId.of(id), staffId(authentication));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        markAllRead.markAllRead(staffId(authentication));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> unreadCount(Authentication authentication) {
        return ResponseEntity.ok(new UnreadCountResponse(getUnreadCount.getUnreadCount(staffId(authentication))));
    }

    @GetMapping("/templates")
    @PreAuthorize("hasAuthority('SETTINGS_VIEW')")
    public ResponseEntity<List<NotificationTemplateResponse>> listTemplates() {
        return ResponseEntity.ok(listTemplates.list().stream().map(mapper::toResponse).toList());
    }

    @PostMapping("/templates")
    @PreAuthorize("hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<NotificationTemplateCreatedResponse> createTemplate(
            @Valid @RequestBody CreateTemplateRequest request,
            Authentication authentication) {
        NotificationTemplateId id = createTemplate.create(mapper.toCommand(request, staffId(authentication)));
        return ResponseEntity.status(HttpStatus.CREATED).body(new NotificationTemplateCreatedResponse(id.value()));
    }

    @PutMapping("/templates/{id}")
    @PreAuthorize("hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<Void> updateTemplate(@PathVariable("id") UUID id,
                                               @Valid @RequestBody UpdateTemplateRequest request,
                                               Authentication authentication) {
        updateTemplate.update(mapper.toCommand(id, request, staffId(authentication)));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/automation-rules")
    @PreAuthorize("hasAuthority('SETTINGS_VIEW')")
    public ResponseEntity<List<AutomationRuleResponse>> listRules() {
        return ResponseEntity.ok(listRules.list().stream().map(mapper::toResponse).toList());
    }

    @PostMapping("/automation-rules")
    @PreAuthorize("hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<AutomationRuleCreatedResponse> createRule(
            @Valid @RequestBody CreateAutomationRuleRequest request,
            Authentication authentication) {
        AutomationRuleId id = createRule.create(mapper.toCommand(request, staffId(authentication)));
        return ResponseEntity.status(HttpStatus.CREATED).body(new AutomationRuleCreatedResponse(id.value()));
    }

    @PatchMapping("/automation-rules/{id}/toggle")
    @PreAuthorize("hasAuthority('SETTINGS_EDIT')")
    public ResponseEntity<Void> toggleRule(@PathVariable("id") UUID id) {
        toggleRule.toggle(AutomationRuleId.of(id));
        return ResponseEntity.noContent().build();
    }

    private static StaffId staffId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof StaffPrincipal principal) {
            return principal.staffId();
        }
        return null;
    }
}
