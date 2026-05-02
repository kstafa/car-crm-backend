package com.rentflow.reservation.adapter.in.rest;

import com.rentflow.fleet.model.AvailableVehicle;
import com.rentflow.fleet.port.in.FindAvailableVehiclesUseCase;
import com.rentflow.fleet.query.FindAvailableVehiclesQuery;
import com.rentflow.reservation.DateRange;
import com.rentflow.reservation.ReservationStatus;
import com.rentflow.reservation.command.CreateReservationCommand;
import com.rentflow.reservation.model.CalendarEntry;
import com.rentflow.reservation.model.ConflictSummary;
import com.rentflow.reservation.model.ReservationDetail;
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
import com.rentflow.reservation.query.GetCalendarQuery;
import com.rentflow.security.JwtAuthFilter;
import com.rentflow.security.JwtTokenService;
import com.rentflow.security.SecurityConfig;
import com.rentflow.shared.BlacklistedCustomerException;
import com.rentflow.shared.ResourceNotFoundException;
import com.rentflow.shared.VehicleNotAvailableException;
import com.rentflow.shared.adapter.in.GlobalExceptionHandler;
import com.rentflow.shared.adapter.in.rest.PageMeta;
import com.rentflow.shared.adapter.in.rest.PageResponse;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;
import com.rentflow.shared.money.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
@Import({ReservationController.class, SecurityConfig.class, JwtAuthFilter.class, JwtTokenService.class,
        GlobalExceptionHandler.class})
@TestPropertySource(properties = "rentflow.jwt.secret=01234567890123456789012345678901")
class ReservationControllerTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final ZonedDateTime PICKUP = ZonedDateTime.of(2026, 8, 1, 9, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateReservationUseCase createReservation;
    @MockBean
    private ConfirmReservationUseCase confirmReservation;
    @MockBean
    private CancelReservationUseCase cancelReservation;
    @MockBean
    private ExtendReservationUseCase extendReservation;
    @MockBean
    private ApplyDiscountUseCase applyDiscount;
    @MockBean
    private GetReservationUseCase getReservation;
    @MockBean
    private ListReservationsUseCase listReservations;
    @MockBean
    private GetReservationCalendarUseCase getCalendar;
    @MockBean
    private GetReservationConflictsUseCase getConflicts;
    @MockBean
    private ListTodayPickupsUseCase listTodayPickups;
    @MockBean
    private ListTodayReturnsUseCase listTodayReturns;
    @MockBean
    private ListOverdueUseCase listOverdue;
    @MockBean
    private FindAvailableVehiclesUseCase findAvailable;
    @MockBean
    private ReservationMapper mapper;

    @Test
    @WithMockUser(authorities = "RESERVATION_VIEW")
    void listReservations_authenticated_returns200WithPage() throws Exception {
        ReservationSummary summary = reservationSummary();
        ReservationListResponse response = listResponse(summary.id().value());
        when(listReservations.list(any())).thenReturn(new PageImpl<>(List.of(summary)));
        when(mapper.toPageResponse(any())).thenReturn(new PageResponse<>(List.of(response),
                new PageMeta(0, 1, 1, 1)));

        mockMvc.perform(get("/api/v1/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(summary.id().value().toString()));
    }

    @Test
    void listReservations_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/reservations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER_VIEW")
    void listReservations_missingPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/reservations"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "RESERVATION_VIEW")
    void getReservation_exists_returns200WithDetail() throws Exception {
        ReservationId id = ReservationId.generate();
        ReservationDetail detail = reservationDetail(id);
        when(getReservation.get(id)).thenReturn(detail);
        when(mapper.toDetailResponse(detail)).thenReturn(detailResponse(id.value()));

        mockMvc.perform(get("/api/v1/reservations/{id}", id.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.value().toString()));
    }

    @Test
    @WithMockUser(authorities = "RESERVATION_VIEW")
    void getReservation_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(getReservation.get(ReservationId.of(id))).thenThrow(new ResourceNotFoundException("Reservation not found"));

        mockMvc.perform(get("/api/v1/reservations/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getReservation_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "RESERVATION_CREATE")
    void createReservation_validRequest_returns201WithLocationHeader() throws Exception {
        ReservationId id = ReservationId.generate();
        when(mapper.toCommand(any(CreateReservationRequest.class), any())).thenReturn(createCommand());
        when(createReservation.create(any())).thenReturn(id);

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateJson()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/reservations/" + id.value()))
                .andExpect(jsonPath("$.id").value(id.value().toString()));
    }

    @Test
    @WithMockUser(authorities = "RESERVATION_VIEW")
    void createReservation_missingPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "RESERVATION_CREATE")
    void createReservation_nullCustomerId_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"vehicleId":"%s","pickupDatetime":"%s","returnDatetime":"%s"}
                                """.formatted(VehicleId.generate().value(), PICKUP, PICKUP.plusDays(3))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.customerId").exists());
    }

    @Test
    @WithMockUser(authorities = "RESERVATION_CREATE")
    void createReservation_nullVehicleId_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"%s","pickupDatetime":"%s","returnDatetime":"%s"}
                                """.formatted(CustomerId.generate().value(), PICKUP, PICKUP.plusDays(3))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.vehicleId").exists());
    }

    @Test
    @WithMockUser(authorities = "RESERVATION_CREATE")
    void createReservation_returnBeforePickup_returns400() throws Exception {
        when(mapper.toCommand(any(CreateReservationRequest.class), any()))
                .thenThrow(new IllegalArgumentException("returnDatetime must be after pickupDatetime"));

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"%s","vehicleId":"%s","pickupDatetime":"%s","returnDatetime":"%s"}
                                """.formatted(CustomerId.generate().value(), VehicleId.generate().value(), PICKUP,
                                PICKUP.minusDays(1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "RESERVATION_CREATE")
    void createReservation_vehicleUnavailable_returns409() throws Exception {
        VehicleId vehicleId = VehicleId.generate();
        when(mapper.toCommand(any(CreateReservationRequest.class), any())).thenReturn(createCommand(vehicleId));
        when(createReservation.create(any())).thenThrow(new VehicleNotAvailableException(vehicleId,
                new DateRange(PICKUP, PICKUP.plusDays(3))));

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateJson()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "RESERVATION_CREATE")
    void createReservation_blacklistedCustomer_returns422() throws Exception {
        CustomerId customerId = CustomerId.generate();
        when(mapper.toCommand(any(CreateReservationRequest.class), any())).thenReturn(createCommand());
        when(createReservation.create(any())).thenThrow(new BlacklistedCustomerException(customerId));

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateJson()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(authorities = "RESERVATION_EDIT")
    void confirmReservation_validRequest_returns204() throws Exception {
        mockMvc.perform(patch("/api/v1/reservations/{id}/confirm", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "RESERVATION_VIEW")
    void confirmReservation_missingPermission_returns403() throws Exception {
        mockMvc.perform(patch("/api/v1/reservations/{id}/confirm", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "RESERVATION_EDIT")
    void confirmReservation_vehicleNoLongerAvailable_returns409() throws Exception {
        VehicleId vehicleId = VehicleId.generate();
        doThrow(new VehicleNotAvailableException(vehicleId, new DateRange(PICKUP, PICKUP.plusDays(3))))
                .when(confirmReservation).confirm(any());

        mockMvc.perform(patch("/api/v1/reservations/{id}/confirm", UUID.randomUUID()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "RESERVATION_CANCEL")
    void cancelReservation_validReason_returns204() throws Exception {
        mockMvc.perform(patch("/api/v1/reservations/{id}/cancel", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"customer request"}
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "RESERVATION_CANCEL")
    void cancelReservation_blankReason_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/reservations/{id}/cancel", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.reason").exists());
    }

    @Test
    @WithMockUser(authorities = "RESERVATION_VIEW")
    void cancelReservation_missingPermission_returns403() throws Exception {
        mockMvc.perform(patch("/api/v1/reservations/{id}/cancel", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"customer request"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "RESERVATION_EDIT")
    void extendReservation_validDate_returns204() throws Exception {
        mockMvc.perform(patch("/api/v1/reservations/{id}/extend", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newReturnDatetime":"%s"}
                                """.formatted(PICKUP.plusDays(5))))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "RESERVATION_EDIT")
    void extendReservation_vehicleBookedForExtension_returns409() throws Exception {
        VehicleId vehicleId = VehicleId.generate();
        doThrow(new VehicleNotAvailableException(vehicleId, new DateRange(PICKUP.plusDays(3), PICKUP.plusDays(5))))
                .when(extendReservation).extend(any());

        mockMvc.perform(patch("/api/v1/reservations/{id}/extend", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newReturnDatetime":"%s"}
                                """.formatted(PICKUP.plusDays(5))))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "RESERVATION_EDIT")
    void applyDiscount_belowThreshold_agentCanApply_returns204() throws Exception {
        mockMvc.perform(patch("/api/v1/reservations/{id}/discount", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"discountPercent":0.05}
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "RESERVATION_EDIT")
    void applyDiscount_aboveThreshold_agentDenied_returns403() throws Exception {
        mockMvc.perform(patch("/api/v1/reservations/{id}/discount", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"discountPercent":0.20}
                                """))
                .andExpect(status().isForbidden());
        verify(applyDiscount, never()).apply(any());
    }

    @Test
    @WithMockUser(authorities = {"RESERVATION_EDIT", "RESERVATION_APPROVE_DISCOUNT"})
    void applyDiscount_aboveThreshold_managerApproved_returns204() throws Exception {
        mockMvc.perform(patch("/api/v1/reservations/{id}/discount", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"discountPercent":0.20}
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "RESERVATION_EDIT")
    void applyDiscount_negativePercent_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/reservations/{id}/discount", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"discountPercent":-0.01}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "RESERVATION_VIEW")
    void getCalendar_validRange_returns200WithEntries() throws Exception {
        CalendarEntry entry = calendarEntry();
        CalendarEntryResponse response = calendarResponse(entry.reservationId().value());
        when(mapper.toCalendarQuery(any(), any(), nullable(UUID.class))).thenReturn(calendarQuery());
        when(getCalendar.getCalendar(any())).thenReturn(List.of(entry));
        when(mapper.toCalendarResponse(entry)).thenReturn(response);

        mockMvc.perform(get("/api/v1/reservations/calendar")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reservationId").value(entry.reservationId().value().toString()));
    }

    @Test
    @WithMockUser(authorities = "RESERVATION_VIEW")
    void getCalendar_rangeTooLong_returns400() throws Exception {
        when(getCalendar.getCalendar(nullable(GetCalendarQuery.class)))
                .thenThrow(new IllegalArgumentException("Calendar range must not exceed 90 days"));

        mockMvc.perform(get("/api/v1/reservations/calendar")
                        .param("from", "2026-08-01")
                        .param("to", "2026-12-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER_VIEW")
    void getCalendar_missingPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/calendar")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-07"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "RESERVATION_VIEW")
    void getConflicts_returns200WithList() throws Exception {
        ConflictSummary conflict = conflictSummary();
        ConflictSummaryResponse response = conflictResponse(conflict.draftReservationId().value());
        when(getConflicts.getConflicts()).thenReturn(List.of(conflict));
        when(mapper.toConflictResponse(conflict)).thenReturn(response);

        mockMvc.perform(get("/api/v1/reservations/conflicts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].draftReservationId").value(conflict.draftReservationId().value().toString()));
    }

    @Test
    @WithMockUser(authorities = "RESERVATION_VIEW")
    void getTodayPickups_returns200WithList() throws Exception {
        ReservationSummary summary = reservationSummary();
        when(listTodayPickups.listPickups()).thenReturn(List.of(summary));
        when(mapper.toListResponse(summary)).thenReturn(listResponse(summary.id().value()));

        mockMvc.perform(get("/api/v1/reservations/today-pickups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(summary.id().value().toString()));
    }

    @Test
    @WithMockUser(authorities = "RESERVATION_VIEW")
    void getTodayReturns_returns200WithList() throws Exception {
        ReservationSummary summary = reservationSummary();
        when(listTodayReturns.listReturns()).thenReturn(List.of(summary));
        when(mapper.toListResponse(summary)).thenReturn(listResponse(summary.id().value()));

        mockMvc.perform(get("/api/v1/reservations/today-returns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(summary.id().value().toString()));
    }

    @Test
    @WithMockUser(authorities = "RESERVATION_VIEW")
    void getOverdue_returns200WithList() throws Exception {
        ReservationSummary summary = reservationSummary();
        when(listOverdue.listOverdue()).thenReturn(List.of(summary));
        when(mapper.toListResponse(summary)).thenReturn(listResponse(summary.id().value()));

        mockMvc.perform(get("/api/v1/reservations/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(summary.id().value().toString()));
    }

    @Test
    @WithMockUser(authorities = "RESERVATION_VIEW")
    void getAvailability_validParams_returns200WithVehicles() throws Exception {
        VehicleId id = VehicleId.generate();
        AvailableVehicle vehicle = availableVehicle(id);
        when(mapper.toAvailabilityQuery(any(), any(), any())).thenReturn(availabilityQuery());
        when(findAvailable.find(any())).thenReturn(List.of(vehicle));
        when(mapper.toAvailableResponse(vehicle)).thenReturn(availableResponse(id.value()));

        mockMvc.perform(get("/api/v1/reservations/availability")
                        .param("categoryId", VehicleCategoryId.generate().value().toString())
                        .param("pickupDatetime", PICKUP.toString())
                        .param("returnDatetime", PICKUP.plusDays(3).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.value().toString()));
    }

    @Test
    @WithMockUser(authorities = "RESERVATION_VIEW")
    void getAvailability_missingCategoryId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/availability")
                        .param("pickupDatetime", PICKUP.toString())
                        .param("returnDatetime", PICKUP.plusDays(3).toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "RESERVATION_VIEW")
    void getAvailability_returnBeforePickup_returns400() throws Exception {
        when(mapper.toAvailabilityQuery(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("returnDatetime must be after pickupDatetime"));

        mockMvc.perform(get("/api/v1/reservations/availability")
                        .param("categoryId", VehicleCategoryId.generate().value().toString())
                        .param("pickupDatetime", PICKUP.toString())
                        .param("returnDatetime", PICKUP.minusDays(1).toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER_VIEW")
    void getAvailability_missingPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/availability")
                        .param("categoryId", VehicleCategoryId.generate().value().toString())
                        .param("pickupDatetime", PICKUP.toString())
                        .param("returnDatetime", PICKUP.plusDays(3).toString()))
                .andExpect(status().isForbidden());
    }

    private static String validCreateJson() {
        return """
                {"customerId":"%s","vehicleId":"%s","pickupDatetime":"%s","returnDatetime":"%s",
                "extras":[],"notes":"front desk"}
                """.formatted(CustomerId.generate().value(), VehicleId.generate().value(), PICKUP,
                PICKUP.plusDays(3));
    }

    private static CreateReservationCommand createCommand() {
        return createCommand(VehicleId.generate());
    }

    private static CreateReservationCommand createCommand(VehicleId vehicleId) {
        return new CreateReservationCommand(CustomerId.generate(), vehicleId, PICKUP, PICKUP.plusDays(3),
                StaffId.generate());
    }

    private static ReservationSummary reservationSummary() {
        return new ReservationSummary(ReservationId.generate(), "RES-12345678", CustomerId.generate(),
                VehicleId.generate(), PICKUP, PICKUP.plusDays(3), ReservationStatus.CONFIRMED, money("300.00"));
    }

    private static ReservationDetail reservationDetail(ReservationId id) {
        return new ReservationDetail(id, "RES-12345678", CustomerId.generate(), VehicleId.generate(),
                new DateRange(PICKUP, PICKUP.plusDays(3)), ReservationStatus.CONFIRMED, money("300.00"),
                Money.zero(EUR), Money.zero(EUR), Money.zero(EUR), money("300.00"), "front desk");
    }

    private static ReservationListResponse listResponse(UUID id) {
        return new ReservationListResponse(id, "RES-12345678", CustomerId.generate().value(), "Ada Lovelace",
                VehicleId.generate().value(), "AA-123-AA", PICKUP, PICKUP.plusDays(3), "CONFIRMED",
                new BigDecimal("300.00"), "EUR");
    }

    private static ReservationDetailResponse detailResponse(UUID id) {
        return new ReservationDetailResponse(id, "RES-12345678", CustomerId.generate().value(), "Ada Lovelace",
                VehicleId.generate().value(), "AA-123-AA", "Toyota", "Yaris", PICKUP, PICKUP.plusDays(3),
                "CONFIRMED", new BigDecimal("300.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("300.00"), "EUR", "front desk", List.of());
    }

    private static CalendarEntry calendarEntry() {
        return new CalendarEntry(ReservationId.generate(), "RES-12345678", VehicleId.generate(), "AA-123-AA",
                "Toyota", "Yaris", CustomerId.generate(), "Ada Lovelace", PICKUP, PICKUP.plusDays(3),
                ReservationStatus.CONFIRMED);
    }

    private static CalendarEntryResponse calendarResponse(UUID reservationId) {
        return new CalendarEntryResponse(reservationId, "RES-12345678", VehicleId.generate().value(), "AA-123-AA",
                "Toyota", "Yaris", CustomerId.generate().value(), "Ada Lovelace", PICKUP, PICKUP.plusDays(3),
                "CONFIRMED");
    }

    private static GetCalendarQuery calendarQuery() {
        return new GetCalendarQuery(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 7), null);
    }

    private static ConflictSummary conflictSummary() {
        return new ConflictSummary(ReservationId.generate(), "RES-DRAFT", VehicleId.generate(),
                new DateRange(PICKUP, PICKUP.plusDays(3)), ReservationId.generate(), "RES-CONFIRMED",
                ReservationStatus.CONFIRMED);
    }

    private static ConflictSummaryResponse conflictResponse(UUID draftReservationId) {
        return new ConflictSummaryResponse(draftReservationId, "RES-DRAFT", VehicleId.generate().value(), PICKUP,
                PICKUP.plusDays(3), ReservationId.generate().value(), "RES-CONFIRMED", "CONFIRMED");
    }

    private static FindAvailableVehiclesQuery availabilityQuery() {
        return new FindAvailableVehiclesQuery(VehicleCategoryId.generate(), PICKUP, PICKUP.plusDays(3));
    }

    private static AvailableVehicle availableVehicle(VehicleId id) {
        return new AvailableVehicle(id, "AA-123-AA", "Toyota", "Yaris", 2024, "Economy", money("49.99"), null);
    }

    private static AvailableVehicleResponse availableResponse(UUID id) {
        return new AvailableVehicleResponse(id, "AA-123-AA", "Toyota", "Yaris", 2024, "Economy",
                new BigDecimal("49.99"), "EUR");
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), EUR);
    }
}
