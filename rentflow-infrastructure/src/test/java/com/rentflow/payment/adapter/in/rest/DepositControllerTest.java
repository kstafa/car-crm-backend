package com.rentflow.payment.adapter.in.rest;

import com.rentflow.payment.DepositId;
import com.rentflow.payment.DepositStatus;
import com.rentflow.payment.command.ForfeitDepositCommand;
import com.rentflow.payment.command.ReleaseDepositCommand;
import com.rentflow.payment.model.DepositDetail;
import com.rentflow.payment.model.DepositSummary;
import com.rentflow.payment.port.in.ForfeitDepositUseCase;
import com.rentflow.payment.port.in.GetDepositUseCase;
import com.rentflow.payment.port.in.ListDepositsUseCase;
import com.rentflow.payment.port.in.ReleaseDepositUseCase;
import com.rentflow.security.JwtAuthFilter;
import com.rentflow.security.JwtTokenService;
import com.rentflow.security.SecurityConfig;
import com.rentflow.shared.InvalidStateTransitionException;
import com.rentflow.shared.ResourceNotFoundException;
import com.rentflow.shared.adapter.in.GlobalExceptionHandler;
import com.rentflow.shared.adapter.in.rest.PageMeta;
import com.rentflow.shared.adapter.in.rest.PageResponse;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.InvoiceId;
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
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DepositController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtTokenService.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "rentflow.jwt.secret=01234567890123456789012345678901")
class DepositControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetDepositUseCase getDeposit;
    @MockBean
    private ListDepositsUseCase listDeposits;
    @MockBean
    private ReleaseDepositUseCase releaseDeposit;
    @MockBean
    private ForfeitDepositUseCase forfeitDeposit;
    @MockBean
    private DepositMapper mapper;

    @Test
    @WithMockUser(authorities = "PAYMENT_VIEW")
    void listDeposits_authenticated_returns200() throws Exception {
        DepositSummary summary = depositSummary();
        PageResponse<DepositSummaryResponse> response = new PageResponse<>(
                List.of(new DepositSummaryResponse(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        BigDecimal.TEN, "EUR", "HELD", Instant.now(), null)),
                new PageMeta(0, 1, 1, 1));
        when(listDeposits.list(any())).thenReturn(new PageImpl<>(List.of(summary)));
        when(mapper.toPageResponse(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/deposits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("HELD"));
    }

    @Test
    void listDeposits_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/deposits")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "PAYMENT_VIEW")
    void getDeposit_exists_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        DepositDetail detail = depositDetail(id);
        when(getDeposit.get(DepositId.of(id))).thenReturn(detail);
        when(mapper.toDetailResponse(detail)).thenReturn(detailResponse(id));

        mockMvc.perform(get("/api/v1/deposits/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @WithMockUser(authorities = "PAYMENT_VIEW")
    void getDeposit_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(getDeposit.get(DepositId.of(id))).thenThrow(new ResourceNotFoundException("Deposit not found"));

        mockMvc.perform(get("/api/v1/deposits/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "PAYMENT_RECORD")
    void releaseDeposit_heldDeposit_returns204() throws Exception {
        when(mapper.toReleaseCommand(any(), any(), any())).thenReturn(new ReleaseDepositCommand(null, "ok", null));

        mockMvc.perform(post("/api/v1/deposits/{id}/release", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"completed\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "PAYMENT_RECORD")
    void releaseDeposit_notHeld_returns422() throws Exception {
        when(mapper.toReleaseCommand(any(), any(), any())).thenReturn(new ReleaseDepositCommand(null, "ok", null));
        doThrow(new InvalidStateTransitionException("not held")).when(releaseDeposit).release(any());

        mockMvc.perform(post("/api/v1/deposits/{id}/release", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"completed\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(authorities = "PAYMENT_RECORD")
    void releaseDeposit_blankReason_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/deposits/{id}/release", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "PAYMENT_RECORD")
    void forfeitDeposit_heldDeposit_returns204() throws Exception {
        when(mapper.toForfeitCommand(any(), any(), any())).thenReturn(new ForfeitDepositCommand(null, "damage", null));

        mockMvc.perform(post("/api/v1/deposits/{id}/forfeit", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"damage\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "PAYMENT_RECORD")
    void forfeitDeposit_notHeld_returns422() throws Exception {
        when(mapper.toForfeitCommand(any(), any(), any())).thenReturn(new ForfeitDepositCommand(null, "damage", null));
        doThrow(new InvalidStateTransitionException("not held")).when(forfeitDeposit).forfeit(any());

        mockMvc.perform(post("/api/v1/deposits/{id}/forfeit", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"damage\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(authorities = "PAYMENT_VIEW")
    void forfeitDeposit_missingPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/deposits/{id}/forfeit", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"damage\"}"))
                .andExpect(status().isForbidden());
    }

    private static DepositDetailResponse detailResponse(UUID id) {
        return new DepositDetailResponse(id, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN,
                "EUR", "HELD", null, null, Instant.now(), null);
    }

    private static DepositSummary depositSummary() {
        return new DepositSummary(DepositId.generate(), ContractId.generate(), CustomerId.generate(),
                new Money(BigDecimal.TEN, Currency.getInstance("EUR")), DepositStatus.HELD, Instant.now(), null);
    }

    private static DepositDetail depositDetail(UUID id) {
        return new DepositDetail(DepositId.of(id), ContractId.generate(), CustomerId.generate(), InvoiceId.generate(),
                new Money(BigDecimal.TEN, Currency.getInstance("EUR")), DepositStatus.HELD, null, null, Instant.now(),
                null);
    }
}
