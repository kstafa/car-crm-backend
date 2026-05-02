package com.rentflow.contract.adapter.in.rest;

import com.rentflow.contract.ContractStatus;
import com.rentflow.contract.DamageReportId;
import com.rentflow.contract.command.ExtendContractCommand;
import com.rentflow.contract.command.OpenContractCommand;
import com.rentflow.contract.command.RecordPickupCommand;
import com.rentflow.contract.command.RecordReturnCommand;
import com.rentflow.contract.model.ContractDetail;
import com.rentflow.contract.model.ContractSummary;
import com.rentflow.contract.model.ReturnSummary;
import com.rentflow.contract.port.in.ExtendContractUseCase;
import com.rentflow.contract.port.in.GetContractUseCase;
import com.rentflow.contract.port.in.ListActiveContractsUseCase;
import com.rentflow.contract.port.in.ListContractsUseCase;
import com.rentflow.contract.port.in.OpenContractUseCase;
import com.rentflow.contract.port.in.RecordPickupUseCase;
import com.rentflow.contract.port.in.RecordReturnUseCase;
import com.rentflow.contract.port.in.UploadContractPhotoUseCase;
import com.rentflow.security.JwtAuthFilter;
import com.rentflow.security.JwtTokenService;
import com.rentflow.security.SecurityConfig;
import com.rentflow.shared.DomainException;
import com.rentflow.shared.InvalidStateTransitionException;
import com.rentflow.shared.ResourceNotFoundException;
import com.rentflow.shared.VehicleNotAvailableException;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.InvoiceId;
import com.rentflow.shared.id.ReservationId;
import com.rentflow.shared.id.VehicleId;
import com.rentflow.shared.money.Money;
import com.rentflow.shared.adapter.in.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContractController.class)
@Import({ContractController.class, SecurityConfig.class, JwtAuthFilter.class, JwtTokenService.class,
        GlobalExceptionHandler.class})
@TestPropertySource(properties = "rentflow.jwt.secret=01234567890123456789012345678901")
class ContractControllerTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final ZonedDateTime PICKUP = ZonedDateTime.of(2026, 7, 1, 9, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OpenContractUseCase openContract;
    @MockBean
    private RecordPickupUseCase recordPickup;
    @MockBean
    private RecordReturnUseCase recordReturn;
    @MockBean
    private ExtendContractUseCase extendContract;
    @MockBean
    private GetContractUseCase getContract;
    @MockBean
    private ListContractsUseCase listContracts;
    @MockBean
    private ListActiveContractsUseCase listActiveContracts;
    @MockBean
    private UploadContractPhotoUseCase uploadPhoto;
    @MockBean
    private ContractMapper mapper;

    @Test
    @WithMockUser(authorities = "CONTRACT_VIEW")
    void listContracts_authenticated_returns200WithPage() throws Exception {
        ContractSummary summary = summary();
        when(listContracts.list(any())).thenReturn(new PageImpl<>(List.of(summary)));
        when(mapper.toSummaryResponse(summary)).thenReturn(summaryResponse(summary.id().value()));

        mockMvc.perform(get("/api/v1/contracts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(summary.id().value().toString()));
    }

    @Test
    void listContracts_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/contracts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER_VIEW")
    void listContracts_missingPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/contracts"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "CONTRACT_VIEW")
    void getContract_exists_returns200WithDetail() throws Exception {
        ContractId id = ContractId.generate();
        ContractDetail detail = detail(id);
        when(getContract.get(id)).thenReturn(detail);
        when(mapper.toDetailResponse(detail)).thenReturn(detailResponse(id.value()));

        mockMvc.perform(get("/api/v1/contracts/{id}", id.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.value().toString()));
    }

    @Test
    @WithMockUser(authorities = "CONTRACT_VIEW")
    void getContract_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(getContract.get(ContractId.of(id))).thenThrow(new ResourceNotFoundException("Contract not found"));

        mockMvc.perform(get("/api/v1/contracts/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CONTRACT_CREATE")
    void openContract_confirmedReservation_returns201WithLocation() throws Exception {
        ContractId id = ContractId.generate();
        when(mapper.toCommand(any(OpenContractRequest.class), any())).thenReturn(new OpenContractCommand(
                ReservationId.generate(), null));
        when(openContract.open(any())).thenReturn(id);

        mockMvc.perform(post("/api/v1/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationId\":\"%s\"}".formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/contracts/" + id.value()))
                .andExpect(jsonPath("$.id").value(id.value().toString()));
    }

    @Test
    @WithMockUser(authorities = "CONTRACT_VIEW")
    void openContract_missingPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationId\":\"%s\"}".formatted(UUID.randomUUID())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "CONTRACT_CREATE")
    void openContract_nullReservationId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "CONTRACT_CREATE")
    void openContract_reservationNotConfirmed_returns422() throws Exception {
        when(mapper.toCommand(any(OpenContractRequest.class), any())).thenReturn(new OpenContractCommand(
                ReservationId.generate(), null));
        when(openContract.open(any())).thenThrow(new InvalidStateTransitionException("Reservation not confirmed"));

        mockMvc.perform(post("/api/v1/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationId\":\"%s\"}".formatted(UUID.randomUUID())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(authorities = "CONTRACT_CREATE")
    void openContract_contractAlreadyExists_returns422() throws Exception {
        when(mapper.toCommand(any(OpenContractRequest.class), any())).thenReturn(new OpenContractCommand(
                ReservationId.generate(), null));
        when(openContract.open(any())).thenThrow(new DomainException("Contract already exists"));

        mockMvc.perform(post("/api/v1/contracts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationId\":\"%s\"}".formatted(UUID.randomUUID())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(authorities = "CONTRACT_CREATE")
    void recordPickup_validRequest_returns204() throws Exception {
        when(mapper.toCommand(any(), any(RecordPickupRequest.class), any()))
                .thenReturn(new RecordPickupCommand(ContractId.generate(), com.rentflow.contract.InspectionChecklist.allOk(),
                        com.rentflow.shared.FuelLevel.FULL, 1000, List.of(), null));

        mockMvc.perform(post("/api/v1/contracts/{id}/pickup", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pickupJson()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "CONTRACT_VIEW")
    void recordPickup_missingPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/contracts/{id}/pickup", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pickupJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "CONTRACT_CREATE")
    void recordPickup_alreadyPickedUp_returns422() throws Exception {
        when(mapper.toCommand(any(), any(RecordPickupRequest.class), any()))
                .thenReturn(new RecordPickupCommand(ContractId.generate(), com.rentflow.contract.InspectionChecklist.allOk(),
                        com.rentflow.shared.FuelLevel.FULL, 1000, List.of(), null));
        doThrow(new InvalidStateTransitionException("Pickup already recorded")).when(recordPickup).recordPickup(any());

        mockMvc.perform(post("/api/v1/contracts/{id}/pickup", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pickupJson()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(authorities = "CONTRACT_CLOSE")
    void recordReturn_noDamage_returns200WithZeroSurcharges() throws Exception {
        ReturnSummary summary = new ReturnSummary(ContractId.generate(), false, null, money("0.00"), money("0.00"),
                money("0.00"), InvoiceId.generate());
        when(mapper.toCommand(any(), any(RecordReturnRequest.class), any())).thenReturn(returnCommand());
        when(recordReturn.recordReturn(any())).thenReturn(summary);
        when(mapper.toResponse(summary)).thenReturn(returnResponse(summary, null));

        mockMvc.perform(post("/api/v1/contracts/{id}/return", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(returnJson(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.damageDetected").value(false))
                .andExpect(jsonPath("$.lateFee").value(0.00));
    }

    @Test
    @WithMockUser(authorities = "CONTRACT_CLOSE")
    void recordReturn_withDamage_returns200WithDamageFlag() throws Exception {
        DamageReportId reportId = DamageReportId.generate();
        ReturnSummary summary = new ReturnSummary(ContractId.generate(), true, reportId, money("0.00"), money("0.00"),
                money("0.00"), InvoiceId.generate());
        when(mapper.toCommand(any(), any(RecordReturnRequest.class), any())).thenReturn(returnCommand());
        when(recordReturn.recordReturn(any())).thenReturn(summary);
        when(mapper.toResponse(summary)).thenReturn(returnResponse(summary, reportId.value()));

        mockMvc.perform(post("/api/v1/contracts/{id}/return", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(returnJson(true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.damageDetected").value(true))
                .andExpect(jsonPath("$.damageReportId").value(reportId.value().toString()));
    }

    @Test
    @WithMockUser(authorities = "CONTRACT_VIEW")
    void recordReturn_missingPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/contracts/{id}/return", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(returnJson(false)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "CONTRACT_CREATE")
    void extendContract_validDate_returns204() throws Exception {
        when(mapper.toCommand(any(), any(ExtendContractRequest.class), any()))
                .thenReturn(new ExtendContractCommand(ContractId.generate(), PICKUP.plusDays(5), null));

        mockMvc.perform(patch("/api/v1/contracts/{id}/extend", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newScheduledReturn\":\"%s\"}".formatted(PICKUP.plusDays(5))))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "CONTRACT_CREATE")
    void extendContract_vehicleNotAvailable_returns409() throws Exception {
        when(mapper.toCommand(any(), any(ExtendContractRequest.class), any()))
                .thenReturn(new ExtendContractCommand(ContractId.generate(), PICKUP.plusDays(5), null));
        doThrow(new VehicleNotAvailableException(VehicleId.generate(),
                new com.rentflow.reservation.DateRange(PICKUP, PICKUP.plusDays(1)))).when(extendContract).extend(any());

        mockMvc.perform(patch("/api/v1/contracts/{id}/extend", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newScheduledReturn\":\"%s\"}".formatted(PICKUP.plusDays(5))))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "CONTRACT_CREATE")
    void uploadPhoto_validMultipart_returns200WithKey() throws Exception {
        when(uploadPhoto.upload(any())).thenReturn("contracts/a.jpg");
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1});

        mockMvc.perform(multipart("/api/v1/contracts/{id}/photos", UUID.randomUUID()).file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("contracts/a.jpg"));
    }

    @Test
    @WithMockUser(authorities = "CONTRACT_VIEW")
    void uploadPhoto_missingPermission_returns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1});

        mockMvc.perform(multipart("/api/v1/contracts/{id}/photos", UUID.randomUUID()).file(file))
                .andExpect(status().isForbidden());
    }

    private static ContractSummary summary() {
        return new ContractSummary(ContractId.generate(), "CON-ABCDEFGH", ReservationId.generate(),
                CustomerId.generate(), "Ada Lovelace", VehicleId.generate(), "AA-123-AA", PICKUP,
                PICKUP.plusDays(3), null, null, ContractStatus.ACTIVE);
    }

    private static ContractDetail detail(ContractId id) {
        return new ContractDetail(id, "CON-ABCDEFGH", ReservationId.generate(), CustomerId.generate(),
                VehicleId.generate(), PICKUP, PICKUP.plusDays(3), null, null, ContractStatus.ACTIVE, null, null, null);
    }

    private static ContractSummaryResponse summaryResponse(UUID id) {
        return new ContractSummaryResponse(id, "CON-ABCDEFGH", UUID.randomUUID(), UUID.randomUUID(), "Ada Lovelace",
                UUID.randomUUID(), "AA-123-AA", PICKUP, PICKUP.plusDays(3), null, null, "ACTIVE");
    }

    private static ContractDetailResponse detailResponse(UUID id) {
        return new ContractDetailResponse(id, "CON-ABCDEFGH", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                PICKUP, PICKUP.plusDays(3), null, null, "ACTIVE", null, null, null);
    }

    private static ReturnSummaryResponse returnResponse(ReturnSummary summary, UUID reportId) {
        return new ReturnSummaryResponse(summary.contractId().value(), summary.damageDetected(), reportId,
                summary.lateFee().amount(), summary.fuelSurcharge().amount(), summary.totalSurcharges().amount(),
                "EUR", summary.invoiceId().value());
    }

    private static RecordReturnCommand returnCommand() {
        return new RecordReturnCommand(ContractId.generate(), com.rentflow.contract.InspectionChecklist.allOk(),
                com.rentflow.shared.FuelLevel.FULL, 1000, List.of(), null, null, null, null, null);
    }

    private static Money money(String amount) {
        return new Money(new BigDecimal(amount), EUR);
    }

    private static String pickupJson() {
        return """
                {"preInspection":{"frontOk":true,"rearOk":true,"leftSideOk":true,"rightSideOk":true,
                "interiorOk":true,"trunkOk":true,"tiresOk":true,"lightsOk":true,"notes":"ok"},
                "startFuelLevel":"FULL","startMileage":1000,"photoKeys":[]}
                """;
    }

    private static String returnJson(boolean damage) {
        if (!damage) {
            return """
                    {"postInspection":{"frontOk":true,"rearOk":true,"leftSideOk":true,"rightSideOk":true,
                    "interiorOk":true,"trunkOk":true,"tiresOk":true,"lightsOk":true,"notes":"ok"},
                    "endFuelLevel":"FULL","endMileage":1100,"photoKeys":[]}
                    """;
        }
        return """
                {"postInspection":{"frontOk":false,"rearOk":true,"leftSideOk":true,"rightSideOk":true,
                "interiorOk":true,"trunkOk":true,"tiresOk":true,"lightsOk":true,"notes":"dent"},
                "endFuelLevel":"FULL","endMileage":1100,"photoKeys":[],"damageDescription":"Dent",
                "damageSeverity":"MINOR","damageLiability":"CUSTOMER","estimatedDamageCost":250.00}
                """;
    }
}
