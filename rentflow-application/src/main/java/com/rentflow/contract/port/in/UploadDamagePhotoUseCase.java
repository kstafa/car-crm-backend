package com.rentflow.contract.port.in;

import com.rentflow.contract.command.UploadDamagePhotoCommand;

public interface UploadDamagePhotoUseCase {
    String upload(UploadDamagePhotoCommand command);
}
