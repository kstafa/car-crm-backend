package com.rentflow.customer.adapter.in.rest;

import com.rentflow.customer.CustomerStatus;
import com.rentflow.customer.command.CreateCustomerCommand;
import com.rentflow.customer.model.CustomerDetail;
import com.rentflow.customer.model.CustomerSummary;
import com.rentflow.customer.port.in.BlacklistCustomerUseCase;
import com.rentflow.customer.port.in.CreateCustomerUseCase;
import com.rentflow.customer.port.in.GetCustomerUseCase;
import com.rentflow.customer.port.in.ListCustomersUseCase;
import com.rentflow.customer.port.in.ReactivateCustomerUseCase;
import com.rentflow.security.JwtAuthFilter;
import com.rentflow.security.JwtTokenService;
import com.rentflow.security.SecurityConfig;
import com.rentflow.shared.DomainException;
import com.rentflow.shared.InvalidStateTransitionException;
import com.rentflow.shared.ResourceNotFoundException;
import com.rentflow.shared.adapter.in.GlobalExceptionHandler;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.StaffId;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@Import({CustomerController.class, SecurityConfig.class, JwtAuthFilter.class, JwtTokenService.class,
        GlobalExceptionHandler.class})
@TestPropertySource(properties = "rentflow.jwt.secret=01234567890123456789012345678901")
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateCustomerUseCase createCustomer;
    @MockBean
    private GetCustomerUseCase getCustomer;
    @MockBean
    private ListCustomersUseCase listCustomers;
    @MockBean
    private BlacklistCustomerUseCase blacklistCustomer;
    @MockBean
    private ReactivateCustomerUseCase reactivateCustomer;
    @MockBean
    private CustomerMapper mapper;

    @Test
    @WithMockUser(authorities = "CUSTOMER_VIEW")
    void listCustomers_authenticated_returns200() throws Exception {
        CustomerSummary summary = customerSummary();
        when(listCustomers.list(any())).thenReturn(new PageImpl<>(List.of(summary)));
        when(mapper.toListResponse(summary)).thenReturn(customerListResponse(summary.id().value()));

        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("ada@example.com"));
    }

    @Test
    void listCustomers_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER_CREATE")
    void createCustomer_validRequest_returns201WithLocationHeader() throws Exception {
        CustomerId id = CustomerId.generate();
        when(mapper.toCommand(any(CreateCustomerRequest.class), any())).thenReturn(createCommand());
        when(createCustomer.create(any())).thenReturn(id);

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCustomerJson("ada@example.com")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/customers/" + id.value()))
                .andExpect(jsonPath("$.id").value(id.value().toString()));
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER_CREATE")
    void createCustomer_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCustomerJson("not-an-email")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER_CREATE")
    void createCustomer_duplicateEmail_returns422() throws Exception {
        when(mapper.toCommand(any(CreateCustomerRequest.class), any())).thenReturn(createCommand());
        when(createCustomer.create(any())).thenThrow(new DomainException("Email already in use"));

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCustomerJson("ada@example.com")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER_VIEW")
    void createCustomer_missingPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCustomerJson("ada@example.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER_VIEW")
    void getCustomer_exists_returns200() throws Exception {
        CustomerId id = CustomerId.generate();
        CustomerDetail detail = customerDetail(id, CustomerStatus.ACTIVE, null);
        when(getCustomer.get(id)).thenReturn(detail);
        when(mapper.toDetailResponse(detail)).thenReturn(customerDetailResponse(id.value(), "ACTIVE", null));

        mockMvc.perform(get("/api/v1/customers/{id}", id.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.value().toString()));
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER_VIEW")
    void getCustomer_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(getCustomer.get(CustomerId.of(id))).thenThrow(new ResourceNotFoundException("Customer not found"));

        mockMvc.perform(get("/api/v1/customers/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER_BLACKLIST")
    void blacklistCustomer_validRequest_returns204() throws Exception {
        mockMvc.perform(patch("/api/v1/customers/{id}/blacklist", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"fraud"}
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER_VIEW")
    void blacklistCustomer_missingPermission_returns403() throws Exception {
        mockMvc.perform(patch("/api/v1/customers/{id}/blacklist", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"fraud"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER_BLACKLIST")
    void blacklistCustomer_alreadyBlacklisted_returns422() throws Exception {
        doThrow(new DomainException("Customer is already blacklisted")).when(blacklistCustomer).blacklist(any());

        mockMvc.perform(patch("/api/v1/customers/{id}/blacklist", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"fraud"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER_EDIT")
    void reactivateCustomer_blacklisted_returns204() throws Exception {
        mockMvc.perform(patch("/api/v1/customers/{id}/reactivate", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER_EDIT")
    void reactivateCustomer_alreadyActive_returns422() throws Exception {
        doThrow(new InvalidStateTransitionException("Only blacklisted or inactive customers can be reactivated"))
                .when(reactivateCustomer).reactivate(any());

        mockMvc.perform(patch("/api/v1/customers/{id}/reactivate", UUID.randomUUID()))
                .andExpect(status().isUnprocessableEntity());
    }

    private static String validCustomerJson(String email) {
        return """
                {"firstName":"Ada","lastName":"Lovelace","email":"%s","phone":"+33123456789"}
                """.formatted(email);
    }

    private static CreateCustomerCommand createCommand() {
        return new CreateCustomerCommand("Ada", "Lovelace", "ada@example.com", "+33123456789",
                StaffId.generate());
    }

    private static CustomerSummary customerSummary() {
        return new CustomerSummary(CustomerId.generate(), "Ada", "Lovelace", "ada@example.com",
                "+33123456789", CustomerStatus.ACTIVE);
    }

    private static CustomerListResponse customerListResponse(UUID id) {
        return new CustomerListResponse(id, "Ada", "Lovelace", "ada@example.com", "+33123456789", "ACTIVE");
    }

    private static CustomerDetail customerDetail(CustomerId id, CustomerStatus status, String blacklistReason) {
        return new CustomerDetail(id, "Ada", "Lovelace", "ada@example.com", "+33123456789", status,
                blacklistReason);
    }

    private static CustomerDetailResponse customerDetailResponse(UUID id, String status, String blacklistReason) {
        return new CustomerDetailResponse(id, "Ada", "Lovelace", "ada@example.com", "+33123456789", status,
                blacklistReason);
    }
}
