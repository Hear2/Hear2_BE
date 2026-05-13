package com.hear2.media.service;

import com.hear2.media.dto.MediaPresignedUrlRequest;
import com.hear2.media.dto.MediaPresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaPresignedUrlService {

    private static final String R2 = "r2";

    private final ObjectProvider<S3Presigner> s3PresignerProvider;

    @Value("${app.media-storage.type:local}")
    private String storageType;

    @Value("${app.media-storage.presigned-upload-expiration-minutes:10}")
    private long uploadExpirationMinutes;

    @Value("${cloudflare.r2.bucket:}")
    private String r2Bucket;

    public MediaPresignedUrlResponse createUploadUrl(MediaPresignedUrlRequest request, Long currentUserId) {
        validate(request, currentUserId);
        if (!R2.equals(storageType.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "media presigned upload requires MEDIA_STORAGE_TYPE=r2");
        }

        S3Presigner presigner = s3PresignerProvider.getIfAvailable();
        if (presigner == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "R2 presigner is not configured");
        }
        if (!StringUtils.hasText(r2Bucket)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "cloudflare.r2.bucket is required");
        }

        String objectKey = buildObjectKey(request, currentUserId);
        Duration expiration = Duration.ofMinutes(uploadExpirationMinutes);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(r2Bucket)
                .key(objectKey)
                .contentType(request.getContentType())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .putObjectRequest(putObjectRequest)
                .build();

        OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plus(expiration);
        return MediaPresignedUrlResponse.builder()
                .uploadUrl(presigner.presignPutObject(presignRequest).url().toString())
                .method("PUT")
                .headers(Map.of("Content-Type", request.getContentType()))
                .objectKey(objectKey)
                .expiresAt(expiresAt)
                .build();
    }

    private void validate(MediaPresignedUrlRequest request, Long currentUserId) {
        if (currentUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "authentication is required");
        }
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "media presigned url request is required");
        }
        if (!request.getContentType().startsWith("image/") && !request.getContentType().startsWith("audio/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contentType must be image/* or audio/*");
        }
    }

    private String buildObjectKey(MediaPresignedUrlRequest request, Long currentUserId) {
        String purpose = StringUtils.hasText(request.getPurpose())
                ? request.getPurpose().replaceAll("[^a-zA-Z0-9_-]", "").toLowerCase(Locale.ROOT)
                : "media";
        String date = OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE);
        String extension = StringUtils.getFilenameExtension(request.getOriginalFileName());
        String safeExtension = StringUtils.hasText(extension)
                ? "." + extension.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "")
                : "";

        return String.join("/",
                "media",
                purpose,
                String.valueOf(currentUserId),
                date,
                UUID.randomUUID() + safeExtension
        );
    }
}
