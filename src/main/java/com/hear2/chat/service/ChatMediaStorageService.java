package com.hear2.chat.service;

import com.hear2.chat.dto.ChatMediaResponse;
import com.hear2.chat.entity.MessageType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ChatMediaStorageService {

    private static final String LOCAL = "local";
    private static final String R2 = "r2";
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_VIDEO_EXTENSIONS = Set.of("mp4", "mov");
    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_VIDEO_CONTENT_TYPES = Set.of("video/mp4", "video/quicktime");
    private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE_BYTES = 50L * 1024 * 1024;
    private static final Map<MessageType, Set<String>> ALLOWED_EXTENSIONS_BY_TYPE = Map.of(
            MessageType.IMAGE, ALLOWED_IMAGE_EXTENSIONS,
            MessageType.VIDEO, ALLOWED_VIDEO_EXTENSIONS
    );

    private final String storageType;
    private final Path uploadRoot;
    private final S3Client s3Client;
    private final String r2Bucket;
    private final String r2PublicUrl;

    public ChatMediaStorageService(
            @Value("${app.file-storage.type:local}") String storageType,
            @Value("${app.file-storage.upload-dir:uploads}") String uploadDir,
            ObjectProvider<S3Client> s3ClientProvider,
            @Value("${cloudflare.r2.bucket:}") String r2Bucket,
            @Value("${cloudflare.r2.public-url:}") String r2PublicUrl
    ) {
        this.storageType = storageType.toLowerCase(Locale.ROOT);
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.s3Client = s3ClientProvider.getIfAvailable();
        this.r2Bucket = r2Bucket;
        this.r2PublicUrl = r2PublicUrl;
    }

    public ChatMediaResponse store(MultipartFile file) {
        validate(file);

        String contentType = file.getContentType();
        String originalFileName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "media" : file.getOriginalFilename()
        );
        MessageType messageType = resolveMessageType(contentType, originalFileName);
        validateSize(file.getSize(), messageType);
        String extension = resolveExtension(originalFileName, messageType);
        Path relativeDirectory = Paths.get(
                "chat",
                LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        );
        String storedFileName = UUID.randomUUID() + "." + extension;
        String objectKey = relativeDirectory.toString().replace('\\', '/') + "/" + storedFileName;
        String mediaUrl = storeFile(file, contentType, relativeDirectory, storedFileName, objectKey);

        return ChatMediaResponse.builder()
                .messageType(messageType)
                .mediaUrl(mediaUrl)
                .originalFileName(originalFileName)
                .mediaContentType(contentType)
                .mediaSize(file.getSize())
                .build();
    }

    private String storeFile(
            MultipartFile file,
            String contentType,
            Path relativeDirectory,
            String storedFileName,
            String objectKey
    ) {
        if (R2.equals(storageType)) {
            return storeToR2(file, contentType, objectKey);
        }
        if (!LOCAL.equals(storageType)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unsupported file storage type: " + storageType);
        }

        try {
            Path storageDirectory = uploadRoot.resolve(relativeDirectory).normalize();
            Files.createDirectories(storageDirectory);

            Path targetPath = storageDirectory.resolve(storedFileName).normalize();
            if (!targetPath.startsWith(uploadRoot)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file path");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store chat media", e);
        }

        return "/uploads/" + relativeDirectory.toString().replace('\\', '/') + "/" + storedFileName;
    }

    private String storeToR2(MultipartFile file, String contentType, String objectKey) {
        validateR2Config();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(r2Bucket)
                .key(objectKey)
                .contentType(contentType)
                .contentLength(file.getSize())
                .build();

        try (InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(inputStream, file.getSize())
                );
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read chat media", e);
        } catch (S3Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "R2 upload failed: " + safeAwsMessage(e),
                    e
            );
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload chat media to R2", e);
        }

        return r2PublicUrl.replaceAll("/+$", "") + "/" + objectKey;
    }

    private void validateR2Config() {
        if (s3Client == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "R2 S3 client is not configured");
        }
        if (!StringUtils.hasText(r2Bucket)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "cloudflare.r2.bucket is required");
        }
        if (!StringUtils.hasText(r2PublicUrl)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "cloudflare.r2.public-url is required");
        }
    }

    private String safeAwsMessage(S3Exception e) {
        if (StringUtils.hasText(e.awsErrorDetails() == null ? null : e.awsErrorDetails().errorMessage())) {
            return e.awsErrorDetails().errorMessage();
        }

        return e.getMessage();
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Media file is required");
        }

        String contentType = file.getContentType();
        String originalFileName = file.getOriginalFilename();
        if (!StringUtils.hasText(contentType) || !StringUtils.hasText(originalFileName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Media file must have a content type and original filename");
        }
        if (!StringUtils.hasText(StringUtils.getFilenameExtension(originalFileName))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Media file must have an allowed extension");
        }
    }

    private MessageType resolveMessageType(String contentType, String originalFileName) {
        String normalizedContentType = contentType.toLowerCase(Locale.ROOT);
        String extension = StringUtils.getFilenameExtension(originalFileName);
        String normalizedExtension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);

        boolean imageByContentType = ALLOWED_IMAGE_CONTENT_TYPES.contains(normalizedContentType);
        boolean videoByContentType = ALLOWED_VIDEO_CONTENT_TYPES.contains(normalizedContentType);
        boolean imageByExtension = ALLOWED_IMAGE_EXTENSIONS.contains(normalizedExtension);
        boolean videoByExtension = ALLOWED_VIDEO_EXTENSIONS.contains(normalizedExtension);

        if ((imageByContentType || imageByExtension) && (videoByContentType || videoByExtension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File content type and extension do not match");
        }

        if (imageByContentType || imageByExtension) {
            if (!(imageByContentType && imageByExtension)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image files must use jpg, jpeg, png, or webp");
            }
            return MessageType.IMAGE;
        }

        if (videoByContentType || videoByExtension) {
            if (!(videoByContentType && videoByExtension)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Video files must use mp4 or mov");
            }
            return MessageType.VIDEO;
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Only jpg, jpeg, png, webp images and mp4, mov videos are allowed"
        );
    }

    private void validateSize(long fileSize, MessageType messageType) {
        long maxSize = messageType == MessageType.IMAGE ? MAX_IMAGE_SIZE_BYTES : MAX_VIDEO_SIZE_BYTES;
        if (fileSize > maxSize) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "File is too large. Maximum size for " + messageType + " is " + maxSize + " bytes"
            );
        }
    }

    private String resolveExtension(String originalFileName, MessageType messageType) {
        String extension = StringUtils.getFilenameExtension(originalFileName);
        if (StringUtils.hasText(extension)) {
            String normalizedExtension = extension.toLowerCase(Locale.ROOT);
            if (ALLOWED_EXTENSIONS_BY_TYPE.getOrDefault(messageType, Set.of()).contains(normalizedExtension)) {
                return normalizedExtension;
            }
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "File extension is not allowed for " + messageType
        );
    }
}
