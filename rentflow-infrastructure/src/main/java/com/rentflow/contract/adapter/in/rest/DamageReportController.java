package com.rentflow.contract.adapter.in.rest;

import com.rentflow.contract.DamageReportId;
import com.rentflow.contract.command.UploadDamagePhotoCommand;
import com.rentflow.contract.port.in.CreateDamageReportUseCase;
import com.rentflow.contract.port.in.GetDamageReportUseCase;
import com.rentflow.contract.port.in.ListDamageReportsUseCase;
import com.rentflow.contract.port.in.UploadDamagePhotoUseCase;
import com.rentflow.security.StaffPrincipal;
import com.rentflow.shared.id.StaffId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/damage-reports")
@RequiredArgsConstructor
public class DamageReportController {

    private final CreateDamageReportUseCase createReport;
    private final GetDamageReportUseCase getReport;
    private final ListDamageReportsUseCase listReports;
    private final UploadDamagePhotoUseCase uploadPhoto;
    private final DamageReportMapper mapper;

    @GetMapping
    @PreAuthorize("hasAuthority('MAINTENANCE_VIEW')")
    public Page<DamageReportSummaryResponse> list(@RequestParam(name = "status", required = false) String status,
                                                  @RequestParam(name = "vehicleId", required = false) UUID vehicleId,
                                                  @RequestParam(name = "page", defaultValue = "0") int page,
                                                  @RequestParam(name = "size", defaultValue = "20") int size) {
        return listReports.list(mapper.toQuery(status, vehicleId, page, size)).map(mapper::toSummaryResponse);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MAINTENANCE_CREATE')")
    public ResponseEntity<DamageReportCreatedResponse> create(@Valid @RequestBody CreateDamageReportRequest request,
                                                              Authentication authentication) {
        DamageReportId id = createReport.create(mapper.toCommand(request, staffId(authentication)));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").build(id.value());
        return ResponseEntity.created(location).body(new DamageReportCreatedResponse(id.value()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MAINTENANCE_VIEW')")
    public DamageReportDetailResponse get(@PathVariable("id") UUID id) {
        return mapper.toDetailResponse(getReport.get(DamageReportId.of(id)));
    }

    @PostMapping(value = "/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('MAINTENANCE_CREATE')")
    public ResponseEntity<PhotoUploadResponse> upload(@PathVariable("id") UUID id,
                                                      @RequestParam("file") MultipartFile file,
                                                      Authentication authentication) throws IOException {
        String key = uploadPhoto.upload(new UploadDamagePhotoCommand(DamageReportId.of(id), file.getBytes(),
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
