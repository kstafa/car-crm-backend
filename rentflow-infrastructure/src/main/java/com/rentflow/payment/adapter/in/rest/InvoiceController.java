package com.rentflow.payment.adapter.in.rest;

import com.rentflow.payment.InvoiceStatus;
import com.rentflow.payment.RefundId;
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
import com.rentflow.security.StaffPrincipal;
import com.rentflow.shared.adapter.in.rest.PageResponse;
import com.rentflow.shared.id.InvoiceId;
import com.rentflow.shared.id.StaffId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InvoiceController {

    private final GetInvoiceUseCase getInvoice;
    private final ListInvoicesUseCase listInvoices;
    private final SendInvoiceUseCase sendInvoice;
    private final VoidInvoiceUseCase voidInvoice;
    private final RecordPaymentUseCase recordPayment;
    private final GenerateInvoicePdfUseCase generatePdf;
    private final ListRefundsUseCase listRefunds;
    private final RequestRefundUseCase requestRefund;
    private final ApproveRefundUseCase approveRefund;
    private final ProcessRefundUseCase processRefund;
    private final InvoiceMapper mapper;

    @GetMapping("/invoices")
    @PreAuthorize("hasAuthority('PAYMENT_VIEW')")
    public PageResponse<InvoiceSummaryResponse> list(@RequestParam(name = "status", required = false) InvoiceStatus status,
                                                     @RequestParam(name = "customerId", required = false) UUID customerId,
                                                     @RequestParam(name = "contractId", required = false) UUID contractId,
                                                     @RequestParam(name = "from", required = false)
                                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                     @RequestParam(name = "to", required = false)
                                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                     @RequestParam(name = "page", defaultValue = "0") int page,
                                                     @RequestParam(name = "size", defaultValue = "20") int size) {
        return mapper.toPageResponse(listInvoices.list(mapper.toQuery(status, customerId, contractId, from, to,
                page, size)));
    }

    @GetMapping("/invoices/{id}")
    @PreAuthorize("hasAuthority('PAYMENT_VIEW')")
    public InvoiceDetailResponse get(@PathVariable("id") UUID id) {
        return mapper.toDetailResponse(getInvoice.get(InvoiceId.of(id)));
    }

    @PatchMapping("/invoices/{id}/send")
    @PreAuthorize("hasAuthority('PAYMENT_RECORD')")
    public ResponseEntity<Void> send(@PathVariable("id") UUID id, Authentication authentication) {
        sendInvoice.send(mapper.toSendCommand(InvoiceId.of(id), staffId(authentication)));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/invoices/{id}/void")
    @PreAuthorize("hasAuthority('PAYMENT_RECORD')")
    public ResponseEntity<Void> voided(@PathVariable("id") UUID id, Authentication authentication) {
        voidInvoice.voidInvoice(mapper.toVoidCommand(InvoiceId.of(id), staffId(authentication)));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/invoices/{id}/payments")
    @PreAuthorize("hasAuthority('PAYMENT_RECORD')")
    public ResponseEntity<Void> payment(@PathVariable("id") UUID id, @Valid @RequestBody RecordPaymentRequest request,
                                        Authentication authentication) {
        recordPayment.record(mapper.toCommand(InvoiceId.of(id), request, staffId(authentication)));
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/invoices/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('PAYMENT_VIEW')")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable("id") UUID id) {
        byte[] pdf = generatePdf.generate(InvoiceId.of(id));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    @GetMapping("/invoices/{id}/refunds")
    @PreAuthorize("hasAuthority('PAYMENT_VIEW')")
    public PageResponse<RefundSummaryResponse> refunds(@PathVariable("id") UUID id,
                                                       @RequestParam(name = "page", defaultValue = "0") int page,
                                                       @RequestParam(name = "size", defaultValue = "20") int size) {
        return mapper.toRefundPageResponse(listRefunds.list(mapper.toRefundQuery(page, size)));
    }

    @PostMapping("/invoices/{id}/refunds")
    @PreAuthorize("hasAuthority('PAYMENT_RECORD')")
    public ResponseEntity<RefundCreatedResponse> requestRefund(@PathVariable("id") UUID id,
                                                               @Valid @RequestBody RequestRefundRequest request,
                                                               Authentication authentication) {
        RefundId refundId = requestRefund.request(mapper.toCommand(InvoiceId.of(id), request,
                staffId(authentication)));
        return ResponseEntity.status(201).body(new RefundCreatedResponse(refundId.value()));
    }

    @PatchMapping("/refunds/{refundId}/approve")
    @PreAuthorize("hasAuthority('PAYMENT_REFUND_APPROVE')")
    public ResponseEntity<Void> approve(@PathVariable("refundId") UUID refundId, Authentication authentication) {
        approveRefund.approve(mapper.toApproveCommand(RefundId.of(refundId), staffId(authentication)));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/refunds/{refundId}/process")
    @PreAuthorize("hasAuthority('PAYMENT_REFUND_APPROVE')")
    public ResponseEntity<Void> process(@PathVariable("refundId") UUID refundId, Authentication authentication) {
        processRefund.process(mapper.toProcessCommand(RefundId.of(refundId), staffId(authentication)));
        return ResponseEntity.noContent().build();
    }

    private static StaffId staffId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof StaffPrincipal principal) {
            return principal.staffId();
        }
        return null;
    }
}
