package com.rentflow.customer.adapter.in.rest;

import com.rentflow.customer.CustomerStatus;
import com.rentflow.customer.command.BlacklistCustomerCommand;
import com.rentflow.customer.command.ReactivateCustomerCommand;
import com.rentflow.customer.port.in.BlacklistCustomerUseCase;
import com.rentflow.customer.port.in.CreateCustomerUseCase;
import com.rentflow.customer.port.in.GetCustomerUseCase;
import com.rentflow.customer.port.in.ListCustomersUseCase;
import com.rentflow.customer.port.in.ReactivateCustomerUseCase;
import com.rentflow.customer.query.ListCustomersQuery;
import com.rentflow.security.StaffPrincipal;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.StaffId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {
    private final CreateCustomerUseCase createCustomer;
    private final GetCustomerUseCase getCustomer;
    private final ListCustomersUseCase listCustomers;
    private final BlacklistCustomerUseCase blacklistCustomer;
    private final ReactivateCustomerUseCase reactivateCustomer;
    private final CustomerMapper mapper;

    @GetMapping
    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    public Page<CustomerListResponse> listCustomers(
            @RequestParam(name = "status", required = false) CustomerStatus status,
            @RequestParam(name = "searchTerm", required = false) String searchTerm,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return listCustomers.list(new ListCustomersQuery(status, searchTerm, page, size))
                .map(mapper::toListResponse);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CUSTOMER_CREATE')")
    public ResponseEntity<CustomerCreatedResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request,
                                                                  Authentication authentication) {
        CustomerId id = createCustomer.create(mapper.toCommand(request, staffId(authentication)));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").build(id.value());
        return ResponseEntity.created(location).body(new CustomerCreatedResponse(id.value()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    public CustomerDetailResponse getCustomer(@PathVariable("id") UUID id) {
        return mapper.toDetailResponse(getCustomer.get(CustomerId.of(id)));
    }

    @PatchMapping("/{id}/blacklist")
    @PreAuthorize("hasAuthority('CUSTOMER_BLACKLIST')")
    public ResponseEntity<Void> blacklistCustomer(@PathVariable("id") UUID id,
                                                  @Valid @RequestBody BlacklistRequest request,
                                                  Authentication authentication) {
        blacklistCustomer.blacklist(new BlacklistCustomerCommand(CustomerId.of(id), request.reason(),
                staffId(authentication)));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivate")
    @PreAuthorize("hasAuthority('CUSTOMER_EDIT')")
    public ResponseEntity<Void> reactivateCustomer(@PathVariable("id") UUID id, Authentication authentication) {
        reactivateCustomer.reactivate(new ReactivateCustomerCommand(CustomerId.of(id), staffId(authentication)));
        return ResponseEntity.noContent().build();
    }

    private static StaffId staffId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof StaffPrincipal principal) {
            return principal.staffId();
        }
        return null;
    }
}
