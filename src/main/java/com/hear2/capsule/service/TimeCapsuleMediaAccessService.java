package com.hear2.capsule.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TimeCapsuleMediaAccessService {

    private static final String R2 = "r2";

    private final ObjectProvider<S3Presigner> s3PresignerProvider;

    @Value("${app.media-storage.type:local}")
    private String storageType;

    @Value("${app.media-storage.presigned-read-expiration-minutes:10}")
    private long readExpirationMinutes;

    @Value("${cloudflare.r2.bucket:}")
    private String r2Bucket;

    public String createReadUrl(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return null;
        }
        if (!R2.equals(storageType.toLowerCase(Locale.ROOT))) {
            return null;
        }
        if (!StringUtils.hasText(r2Bucket)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "cloudflare.r2.bucket is required");
        }

        S3Presigner presigner = s3PresignerProvider.getIfAvailable();
        if (presigner == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "R2 presigner is not configured");
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(r2Bucket)
                .key(objectKey)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(readExpirationMinutes))
                .getObjectRequest(getObjectRequest)
                .build();

        return presigner.presignGetObject(presignRequest).url().toString();
    }
}
