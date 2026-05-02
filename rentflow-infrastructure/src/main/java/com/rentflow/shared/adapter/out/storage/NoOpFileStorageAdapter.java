package com.rentflow.shared.adapter.out.storage;

import com.rentflow.shared.port.out.FileStoragePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "rentflow.storage.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpFileStorageAdapter implements FileStoragePort {

    @Override
    public String upload(byte[] content, String folder, String filename, String contentType) {
        return folder.endsWith("/") ? folder + filename : folder + "/" + filename;
    }

    @Override
    public void delete(String key) {
    }

    @Override
    public String presignedUrl(String key, Duration ttl) {
        return "http://localhost:9000/" + key;
    }
}
