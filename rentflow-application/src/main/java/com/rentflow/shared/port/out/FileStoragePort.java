package com.rentflow.shared.port.out;

import java.time.Duration;

public interface FileStoragePort {
    String upload(byte[] content, String folder, String filename, String contentType);

    void delete(String key);

    String presignedUrl(String key, Duration ttl);
}
