package com.rentflow.payment.adapter.in.rest;

import com.rentflow.payment.InvoiceStatus;
import com.rentflow.payment.command.ApproveRefundCommand;
import com.rentflow.payment.command.ProcessRefundCommand;
import com.rentflow.payment.command.RecordPaymentCommand;
import com.rentflow.payment.command.RequestRefundCommand;
import com.rentflow.payment.command.SendInvoiceCommand;
import com.rentflow.payment.command.VoidInvoiceCommand;
import com.rentflow.payment.model.InvoiceDetail;
import com.rentflow.payment.model.InvoiceSummary;
import com.rentflow.payment.model.RefundSummary;
import com.rentflow.payment.port.in.ApproveRefundUseCase;
import com.rentflow.payment.port.in.GenerateInvoicePdfUseCase;
import com.rentflow.payment.port.in.GetInvoiceUseCase;
import com.rentflow.payment.port.in.ListInvoicesUseCase;
import com.rentflow.payment.port.in.ListRefundsUseCase;
import com.rentflow.payment.port.in.ProcessRefundUseCase;
import com.rentflow.payment.port.in.RecordPaymentUseCase;
import com.rentflow.payment.port.in.RequestRefundUseCase;
import com.rentflow.payment.port.in.SendInvoiceUseCase;
import com.rentflow.payment.port.in.VoidInvoiceUseCase;
import com.rentflow.security.JwtAuthFilter;
import com.rentflow.security.JwtTokenService;
import com.rentflow.security.SecurityConfig;
import com.rentflow.shared.DomainException;
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
import java.time.LocalDate;
import java.util.Currency;
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

@WebMvcTest(InvoiceController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtTokenService.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = "rentflow.jwt.secret=01234567890123456789012345678901")
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetInvoiceUseCase getInvoice;
    @MockBean
    private ListInvoicesUseCase listInvoices;
    @MockBean
    private SendInvoiceUseCase sendInvoice;
    @MockBean
    private VoidInvoiceUseCase voidInvoice;
    @MockBean
    private RecordPaymentUseCase recordPayment;
    @MockBean
    private GenerateInvoicePdfUseCase generatePdf;
    @MockBean
    private ListRefundsUseCase listRefunds;
    @MockBean
    private RequestRefundUseCase requestRefund;
    @MockBean
    private ApproveRefundUseCase approveRefund;
    @MockBean
    private ProcessRefundUseCase processRefund;
    @MockBean
    private InvoiceMapper mapper;

    @Test
    @WithMockUser(authorities = "PAYMENT_VIEW")
    void listInvoices_authenticated_returns200WithPage() throws Exception {
        InvoiceSummary summary = invoiceSummary();
        PageResponse<InvoiceSummaryResponse> response = new PageResponse<>(
                List.of(new InvoiceSummaryResponse(UUID.randomUUID(), "INV-1", UUID.randomUUID(), UUID.randomUUID(),
                        "SENT", BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN,
                        "EUR", LocalDate.now(), LocalDate.now().plusDays(7))),
                new PageMeta(0, 1, 1, 1));
        when(listInvoices.list(any())).thenReturn(new PageImpl<>(List.of(summary)));
        when(mapper.toPageResponse(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].invoiceNumber").value("INV-1"));
    }

    @Test
    void listInvoices_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/invoices")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "CUSTOMER_VIEW")
    void listInvoices_missingPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/invoices")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "PAYMENT_VIEW")
    void getInvoice_exists_returns200WithDetail() throws Exception {
        UUID id = UUID.randomUUID();
        InvoiceDetail detail = invoiceDetail(id);
        when(getInvoice.get(InvoiceId.of(id))).thenReturn(detail);
        when(mapper.toDetailResponse(detail)).thenReturn(detailResponse(id));

        mockMvc.perform(get("/api/v1/invoices/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    @WithMockUser(authorities = "PAYMENT_VIEW")
    void getInvoice_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(getInvoice.get(InvoiceId.of(id))).thenThrow(new ResourceNotFoundException("Invoice not found"));

        mockMvc.perform(get("/api/v1/invoices/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "PAYMENT_RECORD")
    void sendInvoice_draft_returns204() throws Exception {
        when(mapper.toSendCommand(any(), any())).thenReturn(new SendInvoiceCommand(InvoiceId.generate(), null));

        mockMvc.perform(patch("/api/v1/invoices/{id}/send", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "PAYMENT_RECORD")
    void sendInvoice_notDraft_returns422() throws Exception {
        when(mapper.toSendCommand(any(), any())).thenReturn(new SendInvoiceCommand(InvoiceId.generate(), null));
        doThrow(new InvalidStateTransitionException("not draft")).when(sendInvoice).send(any());

        mockMvc.perform(patch("/api/v1/invoices/{id}/send", UUID.randomUUID()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(authorities = "PAYMENT_VIEW")
    void sendInvoice_missingPermission_returns403() throws Exception {
        mockMvc.perform(patch("/api/v1/invoices/{id}/send", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "PAYMENT_RECORD")
    void voidInvoice_noPriorPayments_returns204() throws Exception {
        when(mapper.toVoidCommand(any(), any())).thenReturn(new VoidInvoiceCommand(InvoiceId.generate(), null));

        mockMvc.perform(patch("/api/v1/invoices/{id}/void", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "PAYMENT_RECORD")
    void voidInvoice_withPriorPayments_returns422() throws Exception {
        when(mapper.toVoidCommand(any(), any())).thenReturn(new VoidInvoiceCommand(InvoiceId.generate(), null));
        doThrow(new DomainException("prior payment")).when(voidInvoice).voidInvoice(any());

        mockMvc.perform(patch("/api/v1/invoices/{id}/void", UUID.randomUUID()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(authorities = "PAYMENT_RECORD")
    void recordPayment_validRequest_returns204() throws Exception {
        when(mapper.toCommand(any(), any(RecordPaymentRequest.class), any()))
                .thenReturn(new RecordPaymentCommand(InvoiceId.generate(), null, null, null, null));

        mockMvc.perform(post("/api/v1/invoices/{id}/payments", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00,\"method\":\"CARD\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "PAYMENT_RECORD")
    void recordPayment_exceedsOutstanding_returns422() throws Exception {
        when(mapper.toCommand(any(), any(RecordPaymentRequest.class), any()))
                .thenReturn(new RecordPaymentCommand(InvoiceId.generate(), null, null, null, null));
        doThrow(new DomainException("exceeds")).when(recordPayment).record(any());

        mockMvc.perform(post("/api/v1/invoices/{id}/payments", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00,\"method\":\"CARD\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(authorities = "PAYMENT_RECORD")
    void recordPayment_nullAmount_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/invoices/{id}/payments", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"CARD\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "PAYMENT_VIEW")
    void recordPayment_missingPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/invoices/{id}/payments", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00,\"method\":\"CARD\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "PAYMENT_VIEW")
    void downloadPdf_validId_returnsBinaryWithPdfContentType() throws Exception {
        UUID id = UUID.randomUUID();
        when(generatePdf.generate(InvoiceId.of(id))).thenReturn(new byte[]{'%', 'P', 'D', 'F'});

        mockMvc.perform(get("/api/v1/invoices/{id}/pdf", id))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE));
    }

    @Test
    @WithMockUser(authorities = "PAYMENT_VIEW")
    void downloadPdf_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(generatePdf.generate(InvoiceId.of(id))).thenThrow(new ResourceNotFoundException("Invoice not found"));

        mockMvc.perform(get("/api/v1/invoices/{id}/pdf", id)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "PAYMENT_VIEW")
    void listRefunds_validInvoice_returns200WithPage() throws Exception {
        PageResponse<RefundSummaryResponse> response = new PageResponse<>(List.of(), new PageMeta(0, 20, 0, 0));
        when(listRefunds.list(any())).thenReturn(new PageImpl<>(List.of()));
        when(mapper.toRefundPageResponse(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/invoices/{id}/refunds", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(authorities = "PAYMENT_RECORD")
    void requestRefund_paidInvoice_returns201WithRefundId() throws Exception {
        UUID refundId = UUID.randomUUID();
        when(mapper.toCommand(any(), any(RequestRefundRequest.class), any()))
                .thenReturn(new RequestRefundCommand(InvoiceId.generate(), null, null, null, null, null));
        when(requestRefund.request(any())).thenReturn(com.rentflow.payment.RefundId.of(refundId));

        mockMvc.perform(post("/api/v1/invoices/{id}/refunds", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00,\"reason\":\"GOODWILL\",\"method\":\"CARD\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.refundId").value(refundId.toString()));
    }

    @Test
    @WithMockUser(authorities = "PAYMENT_RECORD")
    void requestRefund_unpaidInvoice_returns422() throws Exception {
        when(mapper.toCommand(any(), any(RequestRefundRequest.class), any()))
                .thenReturn(new RequestRefundCommand(InvoiceId.generate(), null, null, null, null, null));
        when(requestRefund.request(any())).thenThrow(new DomainException("unpaid"));

        mockMvc.perform(post("/api/v1/invoices/{id}/refunds", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00,\"reason\":\"GOODWILL\",\"method\":\"CARD\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(authorities = "PAYMENT_VIEW")
    void requestRefund_missingPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/invoices/{id}/refunds", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00,\"reason\":\"GOODWILL\",\"method\":\"CARD\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "PAYMENT_REFUND_APPROVE")
    void approveRefund_pendingRefund_returns204() throws Exception {
        when(mapper.toApproveCommand(any(), any())).thenReturn(new ApproveRefundCommand(null, null));

        mockMvc.perform(patch("/api/v1/refunds/{id}/approve", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "PAYMENT_RECORD")
    void approveRefund_missingApprovePermission_returns403() throws Exception {
        mockMvc.perform(patch("/api/v1/refunds/{id}/approve", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "PAYMENT_REFUND_APPROVE")
    void processRefund_approvedRefund_returns204() throws Exception {
        when(mapper.toProcessCommand(any(), any())).thenReturn(new ProcessRefundCommand(null, null));

        mockMvc.perform(patch("/api/v1/refunds/{id}/process", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    private static InvoiceDetailResponse detailResponse(UUID id) {
        return new InvoiceDetailResponse(id, "INV-1", UUID.randomUUID(), UUID.randomUUID(), "SENT",
                List.of(), BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN,
                "EUR", LocalDate.now(), LocalDate.now().plusDays(7), List.of());
    }

    private static InvoiceSummary invoiceSummary() {
        Currency eur = Currency.getInstance("EUR");
        return new InvoiceSummary(InvoiceId.generate(), "INV-1", ContractId.generate(), CustomerId.generate(),
                InvoiceStatus.SENT, new Money(BigDecimal.TEN, eur), Money.zero(eur), new Money(BigDecimal.TEN, eur),
                LocalDate.now(), LocalDate.now().plusDays(7));
    }

    private static InvoiceDetail invoiceDetail(UUID id) {
        Currency eur = Currency.getInstance("EUR");
        return new InvoiceDetail(InvoiceId.of(id), "INV-1", ContractId.generate(), CustomerId.generate(),
                InvoiceStatus.SENT, List.of(), new Money(BigDecimal.TEN, eur), Money.zero(eur),
                new Money(BigDecimal.TEN, eur), LocalDate.now(), LocalDate.now().plusDays(7), List.of());
    }
}
