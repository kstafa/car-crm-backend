package com.rentflow.fleet.adapter.in.rest;

import com.rentflow.fleet.VehicleStatus;
import com.rentflow.fleet.command.UpdateVehicleStatusCommand;
import com.rentflow.fleet.command.UploadVehiclePhotoCommand;
import com.rentflow.fleet.model.AvailableVehicle;
import com.rentflow.fleet.model.CategorySummary;
import com.rentflow.fleet.model.VehicleSummary;
import com.rentflow.fleet.port.in.CreateCategoryUseCase;
import com.rentflow.fleet.port.in.FindAvailableVehiclesUseCase;
import com.rentflow.fleet.port.in.GetVehicleUseCase;
import com.rentflow.fleet.port.in.ListCategoriesUseCase;
import com.rentflow.fleet.port.in.ListVehiclesUseCase;
import com.rentflow.fleet.port.in.RegisterVehicleUseCase;
import com.rentflow.fleet.port.in.UpdateVehicleStatusUseCase;
import com.rentflow.fleet.port.in.UploadVehiclePhotoUseCase;
import com.rentflow.fleet.query.ListVehiclesQuery;
import com.rentflow.security.StaffPrincipal;
import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.id.VehicleCategoryId;
import com.rentflow.shared.id.VehicleId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/v1/fleet")
@RequiredArgsConstructor
public class FleetController {

    private final RegisterVehicleUseCase registerVehicle;
    private final UpdateVehicleStatusUseCase updateVehicleStatus;
    private final GetVehicleUseCase getVehicle;
    private final ListVehiclesUseCase listVehicles;
    private final FindAvailableVehiclesUseCase findAvailable;
    private final CreateCategoryUseCase createCategory;
    private final ListCategoriesUseCase listCategories;
    private final UploadVehiclePhotoUseCase uploadVehiclePhoto;
    private final FleetMapper mapper;

    @GetMapping("/vehicles")
    @PreAuthorize("hasAuthority('FLEET_VIEW')")
    public Page<VehicleListResponse> listVehicles(@RequestParam(name = "status", required = false) VehicleStatus status,
                                                  @RequestParam(name = "categoryId", required = false) UUID categoryId,
                                                  @RequestParam(name = "activeOnly", defaultValue = "true")
                                                  boolean activeOnly,
                                                  @RequestParam(name = "page", defaultValue = "0") int page,
                                                  @RequestParam(name = "size", defaultValue = "20") int size,
                                                  @RequestParam(name = "sortBy", required = false) String sortBy) {
        ListVehiclesQuery query = new ListVehiclesQuery(status,
                categoryId == null ? null : VehicleCategoryId.of(categoryId), activeOnly, page, size, sortBy);
        return listVehicles.list(query).map(mapper::toListResponse);
    }

    @PostMapping("/vehicles")
    @PreAuthorize("hasAuthority('FLEET_CREATE')")
    public ResponseEntity<VehicleCreatedResponse> registerVehicle(@Valid @RequestBody RegisterVehicleRequest request,
                                                                  Authentication authentication) {
        VehicleId id = registerVehicle.register(mapper.toCommand(request, staffId(authentication)));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").build(id.value());
        return ResponseEntity.created(location).body(new VehicleCreatedResponse(id.value()));
    }

    @GetMapping("/vehicles/{id}")
    @PreAuthorize("hasAuthority('FLEET_VIEW')")
    public VehicleDetailResponse getVehicle(@PathVariable("id") UUID id) {
        return mapper.toDetailResponse(getVehicle.get(VehicleId.of(id)));
    }

    @PatchMapping("/vehicles/{id}/status")
    @PreAuthorize("hasAuthority('FLEET_EDIT')")
    public ResponseEntity<Void> updateStatus(@PathVariable("id") UUID id,
                                             @Valid @RequestBody UpdateVehicleStatusRequest request,
                                             Authentication authentication) {
        updateVehicleStatus.update(new UpdateVehicleStatusCommand(VehicleId.of(id), request.status(),
                staffId(authentication)));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/vehicles/{id}")
    @PreAuthorize("hasAuthority('FLEET_DELETE')")
    public ResponseEntity<Void> deleteVehicle(@PathVariable("id") UUID id, Authentication authentication) {
        updateVehicleStatus.update(new UpdateVehicleStatusCommand(VehicleId.of(id), VehicleStatus.OUT_OF_SERVICE,
                staffId(authentication)));
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/vehicles/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('FLEET_EDIT')")
    public ResponseEntity<PhotoUploadResponse> uploadVehiclePhoto(@PathVariable("id") UUID id,
                                                                  @RequestParam("file") MultipartFile file,
                                                                  Authentication authentication) throws IOException {
        String key = uploadVehiclePhoto.upload(new UploadVehiclePhotoCommand(VehicleId.of(id), file.getBytes(),
                file.getOriginalFilename(), file.getContentType(), staffId(authentication)));
        return ResponseEntity.ok(new PhotoUploadResponse(key));
    }

    @GetMapping("/availability")
    @PreAuthorize("hasAuthority('FLEET_VIEW')")
    public List<AvailableVehicleResponse> availability(@Valid @ModelAttribute AvailabilityRequest request) {
        List<AvailableVehicle> vehicles = findAvailable.find(mapper.toQuery(request));
        return vehicles.stream().map(mapper::toAvailableResponse).toList();
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAuthority('FLEET_VIEW')")
    public List<CategoryResponse> listCategories() {
        List<CategorySummary> categories = listCategories.list();
        return categories.stream().map(mapper::toResponse).toList();
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAuthority('FLEET_CREATE')")
    public ResponseEntity<CategoryCreatedResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request,
                                                                  Authentication authentication) {
        var id = createCategory.create(mapper.toCommand(request, staffId(authentication)));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").build(id.value());
        return ResponseEntity.created(location).body(new CategoryCreatedResponse(id.value()));
    }

    private static StaffId staffId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof StaffPrincipal principal) {
            return principal.staffId();
        }
        return null;
    }
}
