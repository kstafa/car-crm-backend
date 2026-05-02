package com.rentflow.shared.adapter.out.storage;

import com.rentflow.shared.port.out.FileStoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

@Component
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rentflow.storage.enabled", havingValue = "true")
public class S3FileStorageAdapter implements FileStoragePort {

    private final S3Client s3Client;

    @Value("${rentflow.storage.bucket}")
    private String bucketName;

    @Value("${rentflow.storage.base-url:}")
    private String baseUrl;

    @Override
    public String upload(byte[] content, String folder, String filename, String contentType) {
        String key = folder.endsWith("/") ? folder + filename : folder + "/" + filename;
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(contentType)
                        .contentLength((long) content.length)
                        .build(),
                RequestBody.fromBytes(content));
        return key;
    }

    @Override
    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
    }

    @Override
    public String presignedUrl(String key, Duration ttl) {
        S3Presigner presigner = S3Presigner.create();
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(GetObjectRequest.builder().bucket(bucketName).key(key).build())
                .build();
        return presigner.presignGetObject(request).url().toString();
    }
}
