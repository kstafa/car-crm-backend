package com.rentflow.fleet.command;

import com.rentflow.shared.id.StaffId;
import com.rentflow.shared.id.VehicleId;

import java.util.Objects;

public record UploadVehiclePhotoCommand(
        VehicleId vehicleId,
        byte[] fileBytes,
        String originalFilename,
        String contentType,
        StaffId uploadedBy
) {
    public UploadVehiclePhotoCommand {
        Objects.requireNonNull(vehicleId);
        Objects.requireNonNull(fileBytes);
        Objects.requireNonNull(originalFilename);
        Objects.requireNonNull(contentType);
        fileBytes = fileBytes.clone();
    }
}
