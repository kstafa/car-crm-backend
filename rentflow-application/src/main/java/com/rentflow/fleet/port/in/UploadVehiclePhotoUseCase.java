package com.rentflow.fleet.port.in;

import com.rentflow.fleet.command.UploadVehiclePhotoCommand;

public interface UploadVehiclePhotoUseCase {
    String upload(UploadVehiclePhotoCommand command);
}
