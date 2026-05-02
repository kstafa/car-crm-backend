package com.rentflow.contract.adapter.in.rest;

import com.rentflow.contract.ContractStatus;
import com.rentflow.contract.command.UploadPhotoCommand;
import com.rentflow.contract.model.ContractSummary;
import com.rentflow.contract.port.in.ExtendContractUseCase;
import com.rentflow.contract.port.in.GetContractUseCase;
import com.rentflow.contract.port.in.ListActiveContractsUseCase;
import com.rentflow.contract.port.in.ListContractsUseCase;
import com.rentflow.contract.port.in.OpenContractUseCase;
import com.rentflow.contract.port.in.RecordPickupUseCase;
import com.rentflow.contract.port.in.RecordReturnUseCase;
import com.rentflow.contract.port.in.UploadContractPhotoUseCase;
import com.rentflow.contract.query.ListContractsQuery;
import com.rentflow.security.StaffPrincipal;
import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.CustomerId;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.id.VehicleId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final OpenContractUseCase openContract;
    private final RecordPickupUseCase recordPickup;
    private final RecordReturnUseCase recordReturn;
    private final ExtendContractUseCase extendContract;
    private final GetContractUseCase getContract;
    private final ListContractsUseCase listContracts;
    private final ListActiveContractsUseCase listActiveContracts;
    private final UploadContractPhotoUseCase uploadPhoto;
    private final ContractMapper mapper;

    @GetMapping
    @PreAuthorize("hasAuthority('CONTRACT_VIEW')")
    public Page<ContractSummaryResponse> list(@RequestParam(name = "status", required = false) ContractStatus status,
                                              @RequestParam(name = "vehicleId", required = false) UUID vehicleId,
                                              @RequestParam(name = "customerId", required = false) UUID customerId,
                                              @RequestParam(name = "page", defaultValue = "0") int page,
                                              @RequestParam(name = "size", defaultValue = "20") int size) {
        return listContracts.list(new ListContractsQuery(status,
                        vehicleId == null ? null : VehicleId.of(vehicleId),
                        customerId == null ? null : CustomerId.of(customerId), page, size))
                .map(mapper::toSummaryResponse);
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('CONTRACT_VIEW')")
    public List<ContractSummaryResponse> active() {
        List<ContractSummary> contracts = listActiveContracts.listActive();
        return contracts.stream().map(mapper::toSummaryResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTRACT_VIEW')")
    public ContractDetailResponse get(@PathVariable("id") UUID id) {
        return mapper.toDetailResponse(getContract.get(ContractId.of(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CONTRACT_CREATE')")
    public ResponseEntity<ContractCreatedResponse> open(@Valid @RequestBody OpenContractRequest request,
                                                        Authentication authentication) {
        ContractId id = openContract.open(mapper.toCommand(request, staffId(authentication)));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").build(id.value());
        return ResponseEntity.created(location).body(new ContractCreatedResponse(id.value()));
    }

    @PostMapping("/{id}/pickup")
    @PreAuthorize("hasAuthority('CONTRACT_CREATE')")
    public ResponseEntity<Void> pickup(@PathVariable("id") UUID id, @Valid @RequestBody RecordPickupRequest request,
                                       Authentication authentication) {
        recordPickup.recordPickup(mapper.toCommand(ContractId.of(id), request, staffId(authentication)));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasAuthority('CONTRACT_CLOSE')")
    public ReturnSummaryResponse returned(@PathVariable("id") UUID id, @Valid @RequestBody RecordReturnRequest request,
                                          Authentication authentication) {
        return mapper.toResponse(recordReturn.recordReturn(mapper.toCommand(ContractId.of(id), request,
                staffId(authentication))));
    }

    @PatchMapping("/{id}/extend")
    @PreAuthorize("hasAuthority('CONTRACT_CREATE')")
    public ResponseEntity<Void> extend(@PathVariable("id") UUID id, @Valid @RequestBody ExtendContractRequest request,
                                       Authentication authentication) {
        extendContract.extend(mapper.toCommand(ContractId.of(id), request, staffId(authentication)));
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CONTRACT_CREATE')")
    public ResponseEntity<PhotoUploadResponse> upload(@PathVariable("id") UUID id,
                                                      @RequestParam("file") MultipartFile file,
                                                      Authentication authentication) throws IOException {
        String key = uploadPhoto.upload(new UploadPhotoCommand(ContractId.of(id), file.getBytes(),
                file.getOriginalFilename(), file.getContentType(), staffId(authentication)));
        return ResponseEntity.ok(new PhotoUploadResponse(key));
    }

    private static StaffId staffId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof StaffPrincipal principal) {
            return principal.staffId();
        }
        return null;
    }
}
