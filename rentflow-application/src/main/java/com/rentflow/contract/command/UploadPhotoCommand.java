package com.rentflow.contract.command;

import com.rentflow.shared.id.ContractId;
import com.rentflow.shared.id.StaffId;

import java.util.Objects;

public record UploadPhotoCommand(
        ContractId contractId,
        byte[] fileBytes,
        String originalFilename,
        String contentType,
        StaffId uploadedBy
) {
    public UploadPhotoCommand {
        Objects.requireNonNull(contractId);
        Objects.requireNonNull(fileBytes);
        Objects.requireNonNull(originalFilename);
        Objects.requireNonNull(contentType);
        fileBytes = fileBytes.clone();
    }
}
