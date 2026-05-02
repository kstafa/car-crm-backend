package com.rentflow.contract.command;

import com.rentflow.contract.DamageReportId;
import com.rentflow.shared.id.StaffId;

import java.util.Objects;

public record UploadDamagePhotoCommand(
        DamageReportId reportId,
        byte[] fileBytes,
        String originalFilename,
        String contentType,
        StaffId uploadedBy
) {
    public UploadDamagePhotoCommand {
        Objects.requireNonNull(reportId);
        Objects.requireNonNull(fileBytes);
        Objects.requireNonNull(originalFilename);
        Objects.requireNonNull(contentType);
        fileBytes = fileBytes.clone();
    }
}
