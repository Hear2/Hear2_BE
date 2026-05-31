package com.hear2.memory.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
@Slf4j
public class MemoryPhotoStorageService {

    private static final String LOCAL = "local";
    private static final String R2 = "r2";

    private final String storageType;
    private final Path uploadRoot;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String r2Bucket;
    private final long readExpirationMinutes;

    public MemoryPhotoStorageService(
            @Value("${app.memory-storage.type:local}") String storageType,
            @Value("${app.memory-storage.upload-dir:memory-uploads}") String uploadDir,
            ObjectProvider<S3Client> s3ClientProvider,
            ObjectProvider<S3Presigner> s3PresignerProvider,
            @Value("${cloudflare.r2.bucket:}") String r2Bucket,
            @Value("${app.media-storage.presigned-read-expiration-minutes:10}") long readExpirationMinutes
    ) {
        this.storageType = storageType.toLowerCase(Locale.ROOT);
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.s3Client = s3ClientProvider.getIfAvailable();
        this.s3Presigner = s3PresignerProvider.getIfAvailable();
        this.r2Bucket = r2Bucket;
        this.readExpirationMinutes = readExpirationMinutes;
    }

    public MemoryPhotoStorageResult store(MemoryPhotoFile photo, Long coupleId) {
        validate(photo);
        if (coupleId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "coupleId is required");
        }

        String extension = resolveExtension(photo.originalFileName());
        Path relativeDirectory = Paths.get(
                "memories",
                String.valueOf(coupleId),
                LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        );
        String storedFileName = UUID.randomUUID() + "." + extension;
        String objectKey = relativeDirectory.resolve(storedFileName).toString().replace('\\', '/');

        if (R2.equals(storageType)) {
            storeToR2(photo, objectKey);
        } else if (LOCAL.equals(storageType)) {
            storeToLocal(photo, relativeDirectory, storedFileName);
        } else {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unsupported memory storage type: " + storageType);
        }

        return new MemoryPhotoStorageResult(
                objectKey,
                photo.originalFileName(),
                photo.contentType(),
                photo.size()
        );
    }

    public MemoryPhotoStorageResult referenceExternal(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "imageUrl or objectKey is required");
        }

        String normalizedImageUrl = imageUrl.trim();

        return new MemoryPhotoStorageResult(
                normalizedImageUrl,
                resolveExternalFileName(normalizedImageUrl),
                "image/*",
                0L
        );
    }

    public String createReadUrl(String storedPhotoPath) {
        if (!StringUtils.hasText(storedPhotoPath)) {
            return null;
        }
        if (isExternalReference(storedPhotoPath)) {
            return storedPhotoPath;
        }
        if (!R2.equals(storageType) || s3Presigner == null || !StringUtils.hasText(r2Bucket)) {
            if (R2.equals(storageType)) {
                log.warn(
                        "Memory photo presigned read URL cannot be created. objectKey={}, storageType={}, presignerConfigured={}, bucketConfigured={}",
                        storedPhotoPath,
                        storageType,
                        s3Presigner != null,
                        StringUtils.hasText(r2Bucket)
                );
            }
            return null;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(r2Bucket)
                .key(storedPhotoPath)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(readExpirationMinutes))
                .getObjectRequest(getObjectRequest)
                .build();

        try {
            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (RuntimeException e) {
            log.error(
                    "Memory photo presigned read URL creation failed. objectKey={}, storageType={}, bucket={}, expirationMinutes={}, errorType={}, message={}",
                    storedPhotoPath,
                    storageType,
                    r2Bucket,
                    readExpirationMinutes,
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    e
            );
            return null;
        }
    }

    public MemoryPhotoContent load(String storedPhotoPath, String contentType) {
        if (!StringUtils.hasText(storedPhotoPath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "memory photo not found");
        }
        if (isExternalReference(storedPhotoPath)) {
            log.warn("External memory photo was requested through backend byte endpoint. storedPhotoPath={}", storedPhotoPath);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "external memory photo should be loaded from photoUrl");
        }

        if (R2.equals(storageType)) {
            return new MemoryPhotoContent(loadFromR2(storedPhotoPath), contentType);
        }
        if (LOCAL.equals(storageType)) {
            return new MemoryPhotoContent(loadFromLocal(storedPhotoPath), contentType);
        }

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unsupported memory storage type: " + storageType);
    }

    public void delete(String storedPhotoPath) {
        if (!StringUtils.hasText(storedPhotoPath)) {
            return;
        }
        if (isExternalReference(storedPhotoPath)) {
            return;
        }

        if (R2.equals(storageType)) {
            deleteFromR2(storedPhotoPath);
            return;
        }
        if (LOCAL.equals(storageType)) {
            deleteFromLocal(storedPhotoPath);
            return;
        }

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unsupported memory storage type: " + storageType);
    }

    private void storeToLocal(MemoryPhotoFile photo, Path relativeDirectory, String storedFileName) {
        try {
            Path storageDirectory = uploadRoot.resolve(relativeDirectory).normalize();
            Files.createDirectories(storageDirectory);

            Path targetPath = storageDirectory.resolve(storedFileName).normalize();
            if (!targetPath.startsWith(uploadRoot)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid memory photo path");
            }

            Files.write(targetPath, photo.content());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store memory photo", e);
        }
    }

    private void storeToR2(MemoryPhotoFile photo, String objectKey) {
        validateR2Config();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(r2Bucket)
                .key(objectKey)
                .contentType(photo.contentType())
                .contentLength(photo.size())
                .build();

        try {
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(photo.content()));
        } catch (S3Exception e) {
            log.error(
                    "R2 memory photo upload failed. objectKey={}, contentType={}, size={}, statusCode={}, awsMessage={}",
                    objectKey,
                    photo.contentType(),
                    photo.size(),
                    e.statusCode(),
                    safeAwsMessage(e),
                    e
            );
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "R2 upload failed: " + safeAwsMessage(e), e);
        } catch (RuntimeException e) {
            log.error(
                    "Unexpected memory photo upload failure. objectKey={}, contentType={}, size={}, errorType={}, message={}",
                    objectKey,
                    photo.contentType(),
                    photo.size(),
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    e
            );
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload memory photo to R2", e);
        }
    }

    private byte[] loadFromLocal(String storedPhotoPath) {
        try {
            Path targetPath = uploadRoot.resolve(storedPhotoPath).normalize();
            if (!targetPath.startsWith(uploadRoot)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid memory photo path");
            }

            if (!Files.exists(targetPath)) {
                log.warn("Local memory photo file not found. storedPhotoPath={}, resolvedPath={}", storedPhotoPath, targetPath);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "memory photo not found");
            }

            return Files.readAllBytes(targetPath);
        } catch (IOException e) {
            log.error(
                    "Local memory photo load failed. storedPhotoPath={}, uploadRoot={}, errorType={}, message={}",
                    storedPhotoPath,
                    uploadRoot,
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    e
            );
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load memory photo", e);
        }
    }

    private byte[] loadFromR2(String objectKey) {
        validateR2Config();

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(r2Bucket)
                .key(objectKey)
                .build();

        try {
            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
            return objectBytes.asByteArray();
        } catch (S3Exception e) {
            if (e.statusCode() == HttpStatus.NOT_FOUND.value()) {
                log.warn(
                        "R2 memory photo not found. objectKey={}, bucket={}, statusCode={}, awsMessage={}",
                        objectKey,
                        r2Bucket,
                        e.statusCode(),
                        safeAwsMessage(e)
                );
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "memory photo not found", e);
            }
            log.error(
                    "R2 memory photo download failed. objectKey={}, bucket={}, statusCode={}, awsMessage={}",
                    objectKey,
                    r2Bucket,
                    e.statusCode(),
                    safeAwsMessage(e),
                    e
            );
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "R2 download failed: " + safeAwsMessage(e), e);
        } catch (RuntimeException e) {
            log.error(
                    "Unexpected R2 memory photo download failure. objectKey={}, bucket={}, errorType={}, message={}",
                    objectKey,
                    r2Bucket,
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    e
            );
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load memory photo from R2", e);
        }
    }

    private void deleteFromLocal(String storedPhotoPath) {
        try {
            Path targetPath = uploadRoot.resolve(storedPhotoPath).normalize();
            if (!targetPath.startsWith(uploadRoot)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid memory photo path");
            }

            Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete memory photo", e);
        }
    }

    private void deleteFromR2(String objectKey) {
        validateR2Config();

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(r2Bucket)
                .key(objectKey)
                .build();

        try {
            s3Client.deleteObject(deleteObjectRequest);
        } catch (S3Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "R2 delete failed: " + safeAwsMessage(e), e);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete memory photo from R2", e);
        }
    }

    private void validate(MemoryPhotoFile photo) {
        if (photo == null || photo.content() == null || photo.content().length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Memory photo is required");
        }
        if (!StringUtils.hasText(photo.contentType()) || !StringUtils.hasText(photo.originalFileName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Memory photo must have a content type and original filename");
        }
        if (!StringUtils.hasText(StringUtils.getFilenameExtension(photo.originalFileName()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Memory photo must have an allowed extension");
        }
    }

    private String resolveExtension(String originalFileName) {
        String extension = StringUtils.getFilenameExtension(originalFileName);
        return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    }

    private String resolveExternalFileName(String imageUrl) {
        String path = imageUrl;
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }

        String filename = StringUtils.getFilename(path);
        return StringUtils.hasText(filename) ? filename : "memory-photo";
    }

    private boolean isExternalReference(String storedPhotoPath) {
        return storedPhotoPath.startsWith("http://")
                || storedPhotoPath.startsWith("https://")
                || storedPhotoPath.startsWith("s3://");
    }

    private void validateR2Config() {
        if (s3Client == null) {
            log.error("R2 S3 client is not configured for memory photo storage. storageType={}, bucketConfigured={}", storageType, StringUtils.hasText(r2Bucket));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "R2 S3 client is not configured");
        }
        if (!StringUtils.hasText(r2Bucket)) {
            log.error("R2 bucket is not configured for memory photo storage. storageType={}", storageType);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "cloudflare.r2.bucket is required");
        }
    }

    private String safeAwsMessage(S3Exception e) {
        if (StringUtils.hasText(e.awsErrorDetails() == null ? null : e.awsErrorDetails().errorMessage())) {
            return e.awsErrorDetails().errorMessage();
        }

        return e.getMessage();
    }
}
