package com.rentflow.contract.port.in;

import com.rentflow.contract.command.UploadPhotoCommand;

public interface UploadContractPhotoUseCase {
    String upload(UploadPhotoCommand command);
}
