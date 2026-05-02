package com.rentflow.fleet.adapter.in.rest;

import com.rentflow.fleet.VehicleStatus;
import com.rentflow.fleet.command.RegisterVehicleCommand;
import com.rentflow.fleet.model.AvailableVehicle;
import com.rentflow.fleet.model.CategorySummary;
import com.rentflow.fleet.model.VehicleDetail;
import com.rentflow.fleet.model.VehicleSummary;
import com.rentflow.fleet.port.in.CreateCategoryUseCase;
import com.rentflow.fleet.port.in.FindAvailableVehiclesUseCase;
import com.rentflow.fleet.port.in.GetVehicleUseCase;
import com.rentflow.fleet.port.in.ListCategoriesUseCase;
import com.rentflow.fleet.port.in.ListVehiclesUseCase;
import com.rentflow.fleet.port.in.RegisterVehicleUseCase;
import com.rentflow.fleet.port.in.UpdateVehicleStatusUseCase;
import com.rentflow.fleet.port.in.UploadVehiclePhotoUseCase;
import com.rentflow.fleet.query.FindAvailableVehiclesQuery;
import com.rentflow.security.JwtAuthFilter;
import com.rentflow.security.JwtTokenService;
import com.rentflow.security.SecurityConfig;
import com.rentflow.shared.DomainException;
import com.rentflow.shared.ResourceNotFoundException;
import com.rentflow.shared.adapter.in.GlobalExceptionHandler;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;
import com.rentflow.shared.money.Money;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FleetController.class)
@Import({FleetController.class, SecurityConfig.class, JwtAuthFilter.class, JwtTokenService.class,
        GlobalExceptionHandler.class})
@TestPropertySource(properties = "rentflow.jwt.secret=01234567890123456789012345678901")
class FleetControllerTest {

    private static final Currency EUR = Currency.getInstance("EUR");
    private static final ZonedDateTime PICKUP = ZonedDateTime.of(2026, 7, 1, 9, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegisterVehicleUseCase registerVehicle;
    @MockBean
    private UpdateVehicleStatusUseCase updateVehicleStatus;
    @MockBean
    private GetVehicleUseCase getVehicle;
    @MockBean
    private ListVehiclesUseCase listVehicles;
    @MockBean
    private FindAvailableVehiclesUseCase findAvailable;
    @MockBean
    private CreateCategoryUseCase createCategory;
    @MockBean
    private ListCategoriesUseCase listCategories;
    @MockBean
    private UploadVehiclePhotoUseCase uploadVehiclePhoto;
    @MockBean
    private FleetMapper mapper;

    @Test
    @WithMockUser(authorities = "FLEET_VIEW")
    void listVehicles_authenticated_returns200WithPage() throws Exception {
        VehicleSummary summary = vehicleSummary();
        when(listVehicles.list(any())).thenReturn(new PageImpl<>(List.of(summary)));
        when(mapper.toListResponse(summary)).thenReturn(vehicleListResponse(summary.id().value()));

        mockMvc.perform(get("/api/v1/fleet/vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(summary.id().value().toString()));
    }

    @Test
    void listVehicles_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/fleet/vehicles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER_VIEW")
    void listVehicles_missingFleetViewPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/fleet/vehicles"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "FLEET_CREATE")
    void registerVehicle_validRequest_returns201WithLocationHeader() throws Exception {
        VehicleId id = VehicleId.generate();
        when(mapper.toCommand(any(RegisterVehicleRequest.class), any()))
                .thenReturn(registerCommand(VehicleCategoryId.generate()));
        when(registerVehicle.register(any())).thenReturn(id);

        mockMvc.perform(post("/api/v1/fleet/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validVehicleJson(VehicleCategoryId.generate().value())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/fleet/vehicles/" + id.value()))
                .andExpect(jsonPath("$.id").value(id.value().toString()));
    }

    @Test
    @WithMockUser(authorities = "FLEET_VIEW")
    void registerVehicle_missingFleetCreatePermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/fleet/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validVehicleJson(VehicleCategoryId.generate().value())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "FLEET_CREATE")
    void registerVehicle_blankLicensePlate_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/v1/fleet/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"licensePlate":"","brand":"Toyota","model":"Yaris","year":2024,
                                "categoryId":"%s","initialMileage":0}
                                """.formatted(VehicleCategoryId.generate().value())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.licensePlate").exists());
    }

    @Test
    @WithMockUser(authorities = "FLEET_CREATE")
    void registerVehicle_categoryNotFound_returns404() throws Exception {
        when(mapper.toCommand(any(RegisterVehicleRequest.class), any()))
                .thenReturn(registerCommand(VehicleCategoryId.generate()));
        when(registerVehicle.register(any())).thenThrow(new ResourceNotFoundException("Category not found"));

        mockMvc.perform(post("/api/v1/fleet/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validVehicleJson(VehicleCategoryId.generate().value())))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "FLEET_VIEW")
    void getVehicle_exists_returns200WithDetail() throws Exception {
        VehicleId id = VehicleId.generate();
        VehicleDetail detail = vehicleDetail(id);
        when(getVehicle.get(id)).thenReturn(detail);
        when(mapper.toDetailResponse(detail)).thenReturn(vehicleDetailResponse(id.value()));

        mockMvc.perform(get("/api/v1/fleet/vehicles/{id}", id.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.value().toString()));
    }

    @Test
    @WithMockUser(authorities = "FLEET_VIEW")
    void getVehicle_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(getVehicle.get(VehicleId.of(id))).thenThrow(new ResourceNotFoundException("Vehicle not found"));

        mockMvc.perform(get("/api/v1/fleet/vehicles/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "FLEET_EDIT")
    void updateStatus_validRequest_returns204() throws Exception {
        mockMvc.perform(patch("/api/v1/fleet/vehicles/{id}/status", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"MAINTENANCE"}
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "FLEET_EDIT")
    void updateStatus_vehicleNotFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Vehicle not found")).when(updateVehicleStatus).update(any());

        mockMvc.perform(patch("/api/v1/fleet/vehicles/{id}/status", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"MAINTENANCE"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "FLEET_VIEW")
    void getAvailability_validQuery_returns200WithList() throws Exception {
        VehicleId id = VehicleId.generate();
        AvailableVehicle available = availableVehicle(id);
        when(mapper.toQuery(any(AvailabilityRequest.class))).thenReturn(availabilityQuery());
        when(findAvailable.find(any())).thenReturn(List.of(available));
        when(mapper.toAvailableResponse(available)).thenReturn(availableVehicleResponse(id.value()));

        mockMvc.perform(get("/api/v1/fleet/availability")
                        .param("categoryId", VehicleCategoryId.generate().value().toString())
                        .param("pickupDatetime", PICKUP.toString())
                        .param("returnDatetime", PICKUP.plusDays(3).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.value().toString()));
    }

    @Test
    @WithMockUser(authorities = "FLEET_VIEW")
    void getAvailability_returnBeforePickup_returns400() throws Exception {
        when(mapper.toQuery(any(AvailabilityRequest.class)))
                .thenThrow(new IllegalArgumentException("returnDatetime must be after pickupDatetime"));

        mockMvc.perform(get("/api/v1/fleet/availability")
                        .param("categoryId", VehicleCategoryId.generate().value().toString())
                        .param("pickupDatetime", PICKUP.toString())
                        .param("returnDatetime", PICKUP.minusDays(1).toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "FLEET_VIEW")
    void listCategories_returns200WithList() throws Exception {
        CategorySummary summary = categorySummary();
        when(listCategories.list()).thenReturn(List.of(summary));
        when(mapper.toResponse(summary)).thenReturn(categoryResponse(summary.id().value()));

        mockMvc.perform(get("/api/v1/fleet/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Economy"));
    }

    @Test
    @WithMockUser(authorities = "FLEET_CREATE")
    void createCategory_validRequest_returns201() throws Exception {
        VehicleCategoryId id = VehicleCategoryId.generate();
        when(createCategory.create(any())).thenReturn(id);

        mockMvc.perform(post("/api/v1/fleet/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Economy","description":"Small","baseDailyRate":49.99,
                                "depositAmount":300.00,"taxRate":0.20}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.value().toString()));
    }

    @Test
    @WithMockUser(authorities = "FLEET_CREATE")
    void createCategory_duplicateName_returns422() throws Exception {
        when(createCategory.create(any())).thenThrow(new DomainException("Category name already in use"));

        mockMvc.perform(post("/api/v1/fleet/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Economy","description":"Small","baseDailyRate":49.99,
                                "depositAmount":300.00,"taxRate":0.20}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    private static String validVehicleJson(UUID categoryId) {
        return """
                {"licensePlate":"AB-123-CD","brand":"Toyota","model":"Yaris","year":2024,
                "categoryId":"%s","initialMileage":0,"description":"Compact"}
                """.formatted(categoryId);
    }

    private static RegisterVehicleCommand registerCommand(VehicleCategoryId categoryId) {
        return new RegisterVehicleCommand("AB-123-CD", "Toyota", "Yaris", 2024, categoryId, 0, "Compact",
                StaffId.generate());
    }

    private static VehicleSummary vehicleSummary() {
        return new VehicleSummary(VehicleId.generate(), "AB-123-CD", "Toyota", "Yaris", 2024,
                VehicleStatus.AVAILABLE, "Economy", 0, true, null);
    }

    private static VehicleListResponse vehicleListResponse(UUID id) {
        return new VehicleListResponse(id, "AB-123-CD", "Toyota", "Yaris", 2024, "AVAILABLE", "Economy", 0,
                null);
    }

    private static VehicleDetail vehicleDetail(VehicleId id) {
        VehicleCategoryId categoryId = VehicleCategoryId.generate();
        return new VehicleDetail(id, "AB-123-CD", "Toyota", "Yaris", 2024, VehicleStatus.AVAILABLE, categoryId,
                "Economy", 0, true, "Compact", List.of());
    }

    private static VehicleDetailResponse vehicleDetailResponse(UUID id) {
        return new VehicleDetailResponse(id, "AB-123-CD", "Toyota", "Yaris", 2024, "AVAILABLE",
                VehicleCategoryId.generate().value(), "Economy", 0, true, "Compact", List.of());
    }

    private static FindAvailableVehiclesQuery availabilityQuery() {
        return new FindAvailableVehiclesQuery(VehicleCategoryId.generate(), PICKUP, PICKUP.plusDays(3));
    }

    private static AvailableVehicle availableVehicle(VehicleId id) {
        return new AvailableVehicle(id, "AB-123-CD", "Toyota", "Yaris", 2024, "Economy",
                new Money(new BigDecimal("49.99"), EUR), null);
    }

    private static AvailableVehicleResponse availableVehicleResponse(UUID id) {
        return new AvailableVehicleResponse(id, "AB-123-CD", "Toyota", "Yaris", 2024, "Economy",
                new BigDecimal("49.99"), null);
    }

    private static CategorySummary categorySummary() {
        return new CategorySummary(VehicleCategoryId.generate(), "Economy", "Small",
                new Money(new BigDecimal("49.99"), EUR), new Money(new BigDecimal("300.00"), EUR),
                new BigDecimal("0.20"));
    }

    private static CategoryResponse categoryResponse(UUID id) {
        return new CategoryResponse(id, "Economy", "Small", new BigDecimal("49.99"),
                new BigDecimal("300.00"), new BigDecimal("0.20"));
    }
}
