package com.rentflow.reservation.adapter.in.rest;

import com.rentflow.fleet.model.AvailableVehicle;
import com.rentflow.fleet.port.in.FindAvailableVehiclesUseCase;
import com.rentflow.reservation.DiscountPolicy;
import com.rentflow.reservation.ReservationStatus;
import com.rentflow.reservation.command.ApplyDiscountCommand;
import com.rentflow.reservation.command.CancelReservationCommand;
import com.rentflow.reservation.command.ConfirmReservationCommand;
import com.rentflow.reservation.command.ExtendReservationCommand;
import com.rentflow.reservation.model.CalendarEntry;
import com.rentflow.reservation.model.ConflictSummary;
import com.rentflow.reservation.model.ReservationSummary;
import com.rentflow.reservation.port.in.ApplyDiscountUseCase;
import com.rentflow.reservation.port.in.CancelReservationUseCase;
import com.rentflow.reservation.port.in.ConfirmReservationUseCase;
import com.rentflow.reservation.port.in.CreateReservationUseCase;
import com.rentflow.reservation.port.in.ExtendReservationUseCase;
import com.rentflow.reservation.port.in.GetReservationCalendarUseCase;
import com.rentflow.reservation.port.in.GetReservationConflictsUseCase;
import com.rentflow.reservation.port.in.GetReservationUseCase;
import com.rentflow.reservation.port.in.ListOverdueUseCase;
import com.rentflow.reservation.port.in.ListReservationsUseCase;
import com.rentflow.reservation.port.in.ListTodayPickupsUseCase;
import com.rentflow.reservation.port.in.ListTodayReturnsUseCase;
import com.rentflow.reservation.query.ListReservationsQuery;
import com.rentflow.security.StaffPrincipal;
import com.rentflow.shared.adapter.in.rest.PageResponse;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.StaffId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final CreateReservationUseCase createReservation;
    private final ConfirmReservationUseCase confirmReservation;
    private final CancelReservationUseCase cancelReservation;
    private final ExtendReservationUseCase extendReservation;
    private final ApplyDiscountUseCase applyDiscount;
    private final GetReservationUseCase getReservation;
    private final ListReservationsUseCase listReservations;
    private final GetReservationCalendarUseCase getCalendar;
    private final GetReservationConflictsUseCase getConflicts;
    private final ListTodayPickupsUseCase listTodayPickups;
    private final ListTodayReturnsUseCase listTodayReturns;
    private final ListOverdueUseCase listOverdue;
    private final FindAvailableVehiclesUseCase findAvailable;
    private final ReservationMapper mapper;

    @GetMapping
    @PreAuthorize("hasAuthority('RESERVATION_VIEW')")
    public PageResponse<ReservationListResponse> listReservations(
            @RequestParam(name = "status", required = false) ReservationStatus status,
            @RequestParam(name = "from", required = false) LocalDate from,
            @RequestParam(name = "to", required = false) LocalDate to,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        Page<ReservationSummary> result = listReservations.list(new ListReservationsQuery(status, from, to, page,
                size));
        return mapper.toPageResponse(result);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('RESERVATION_CREATE')")
    public ResponseEntity<ReservationCreatedResponse> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            Authentication authentication) {
        ReservationId id = createReservation.create(mapper.toCommand(request, staffId(authentication)));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").build(id.value());
        return ResponseEntity.created(location).body(new ReservationCreatedResponse(id.value()));
    }

    @GetMapping("/availability")
    @PreAuthorize("hasAuthority('RESERVATION_VIEW')")
    public List<AvailableVehicleResponse> availability(
            @RequestParam("categoryId") UUID categoryId,
            @RequestParam("pickupDatetime") ZonedDateTime pickupDatetime,
            @RequestParam("returnDatetime") ZonedDateTime returnDatetime) {
        List<AvailableVehicle> vehicles = findAvailable.find(mapper.toAvailabilityQuery(categoryId, pickupDatetime,
                returnDatetime));
        return vehicles.stream().map(mapper::toAvailableResponse).toList();
    }

    @GetMapping("/calendar")
    @PreAuthorize("hasAuthority('RESERVATION_VIEW')")
    public List<CalendarEntryResponse> calendar(
            @RequestParam("from") LocalDate from,
            @RequestParam("to") LocalDate to,
            @RequestParam(name = "categoryId", required = false) UUID categoryId) {
        List<CalendarEntry> entries = getCalendar.getCalendar(mapper.toCalendarQuery(from, to, categoryId));
        return entries.stream().map(mapper::toCalendarResponse).toList();
    }

    @GetMapping("/conflicts")
    @PreAuthorize("hasAuthority('RESERVATION_VIEW')")
    public List<ConflictSummaryResponse> conflicts() {
        List<ConflictSummary> conflicts = getConflicts.getConflicts();
        return conflicts.stream().map(mapper::toConflictResponse).toList();
    }

    @GetMapping("/today-pickups")
    @PreAuthorize("hasAuthority('RESERVATION_VIEW')")
    public List<ReservationListResponse> todayPickups() {
        return listTodayPickups.listPickups().stream().map(mapper::toListResponse).toList();
    }

    @GetMapping("/today-returns")
    @PreAuthorize("hasAuthority('RESERVATION_VIEW')")
    public List<ReservationListResponse> todayReturns() {
        return listTodayReturns.listReturns().stream().map(mapper::toListResponse).toList();
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAuthority('RESERVATION_VIEW')")
    public List<ReservationListResponse> overdue() {
        return listOverdue.listOverdue().stream().map(mapper::toListResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('RESERVATION_VIEW')")
    public ReservationDetailResponse getReservation(@PathVariable("id") UUID id) {
        return mapper.toDetailResponse(getReservation.get(ReservationId.of(id)));
    }

    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('RESERVATION_EDIT')")
    public ResponseEntity<Void> confirmReservation(@PathVariable("id") UUID id, Authentication authentication) {
        confirmReservation.confirm(new ConfirmReservationCommand(ReservationId.of(id), staffId(authentication)));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('RESERVATION_CANCEL')")
    public ResponseEntity<Void> cancelReservation(@PathVariable("id") UUID id,
                                                  @Valid @RequestBody CancelRequest request,
                                                  Authentication authentication) {
        cancelReservation.cancel(new CancelReservationCommand(ReservationId.of(id), request.reason(),
                staffId(authentication)));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/extend")
    @PreAuthorize("hasAuthority('RESERVATION_EDIT')")
    public ResponseEntity<Void> extendReservation(@PathVariable("id") UUID id,
                                                  @Valid @RequestBody ExtendRequest request,
                                                  Authentication authentication) {
        extendReservation.extend(new ExtendReservationCommand(ReservationId.of(id), request.newReturnDatetime(),
                staffId(authentication)));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/discount")
    @PreAuthorize("hasAnyAuthority('RESERVATION_EDIT', 'RESERVATION_APPROVE_DISCOUNT')")
    public ResponseEntity<Void> applyDiscount(@PathVariable("id") UUID id,
                                              @Valid @RequestBody DiscountRequest request,
                                              Authentication authentication) {
        DiscountPolicy policy = DiscountPolicy.defaultPolicy();
        if (policy.requiresManagerApproval(request.discountPercent())
                && !hasAuthority(authentication, "RESERVATION_APPROVE_DISCOUNT")) {
            BigDecimal percent = policy.maxAgentDiscountPercent().multiply(BigDecimal.valueOf(100));
            throw new AccessDeniedException("Discount above " + percent
                    + "% requires RESERVATION_APPROVE_DISCOUNT permission");
        }

        applyDiscount.apply(new ApplyDiscountCommand(ReservationId.of(id), request.discountPercent(),
                staffId(authentication)));
        return ResponseEntity.noContent().build();
    }

    private static StaffId staffId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof StaffPrincipal principal) {
            return principal.staffId();
        }
        return StaffId.generate();
    }

    private static boolean hasAuthority(Authentication authentication, String authority) {
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }
}
