package com.rentflow.payment.adapter.in.rest;

import com.rentflow.payment.DepositId;
import com.rentflow.payment.DepositStatus;
import com.rentflow.payment.port.in.ForfeitDepositUseCase;
import com.rentflow.payment.port.in.GetDepositUseCase;
import com.rentflow.payment.port.in.ListDepositsUseCase;
import com.rentflow.payment.port.in.ReleaseDepositUseCase;
import com.rentflow.security.StaffPrincipal;
import com.rentflow.shared.adapter.in.rest.PageResponse;
import com.rentflow.shared.id.StaffId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/deposits")
@RequiredArgsConstructor
public class DepositController {

    private final GetDepositUseCase getDeposit;
    private final ListDepositsUseCase listDeposits;
    private final ReleaseDepositUseCase releaseDeposit;
    private final ForfeitDepositUseCase forfeitDeposit;
    private final DepositMapper mapper;

    @GetMapping
    @PreAuthorize("hasAuthority('PAYMENT_VIEW')")
    public PageResponse<DepositSummaryResponse> list(@RequestParam(name = "status", required = false) DepositStatus status,
                                                     @RequestParam(name = "customerId", required = false) UUID customerId,
                                                     @RequestParam(name = "page", defaultValue = "0") int page,
                                                     @RequestParam(name = "size", defaultValue = "20") int size) {
        return mapper.toPageResponse(listDeposits.list(mapper.toQuery(status, customerId, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PAYMENT_VIEW')")
    public DepositDetailResponse get(@PathVariable("id") UUID id) {
        return mapper.toDetailResponse(getDeposit.get(DepositId.of(id)));
    }

    @PostMapping("/{id}/release")
    @PreAuthorize("hasAuthority('PAYMENT_RECORD')")
    public ResponseEntity<Void> release(@PathVariable("id") UUID id, @Valid @RequestBody DepositActionRequest request,
                                        Authentication authentication) {
        releaseDeposit.release(mapper.toReleaseCommand(DepositId.of(id), request, staffId(authentication)));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/forfeit")
    @PreAuthorize("hasAuthority('PAYMENT_RECORD')")
    public ResponseEntity<Void> forfeit(@PathVariable("id") UUID id, @Valid @RequestBody DepositActionRequest request,
                                        Authentication authentication) {
        forfeitDeposit.forfeit(mapper.toForfeitCommand(DepositId.of(id), request, staffId(authentication)));
        return ResponseEntity.noContent().build();
    }

    private static StaffId staffId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof StaffPrincipal principal) {
            return principal.staffId();
        }
        return null;
    }
}
