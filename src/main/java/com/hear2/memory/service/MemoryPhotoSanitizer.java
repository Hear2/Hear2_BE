package com.hear2.memory.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@Service
public class MemoryPhotoSanitizer {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of("image/jpeg", "image/png");
    private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024;

    public MemoryPhotoFile sanitize(MultipartFile photo) {
        validate(photo);

        String originalFileName = StringUtils.cleanPath(
                photo.getOriginalFilename() == null ? "memory-photo" : photo.getOriginalFilename()
        );
        String contentType = photo.getContentType();
        String extension = resolveExtension(originalFileName, contentType);

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(photo.getBytes()));
            if (image == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Memory photo cannot be decoded");
            }

            byte[] sanitizedContent = encodeWithoutMetadata(image, extension);
            if (sanitizedContent.length > MAX_IMAGE_SIZE_BYTES) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sanitized memory photo is too large");
            }

            return new MemoryPhotoFile(sanitizedContent, originalFileName, contentType);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to sanitize memory photo", e);
        }
    }

    private byte[] encodeWithoutMetadata(BufferedImage image, String extension) throws IOException {
        String formatName = normalizeFormatName(extension);
        BufferedImage writableImage = "jpg".equals(formatName) ? removeAlpha(image) : image;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        boolean written = ImageIO.write(writableImage, formatName, outputStream);
        if (!written) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Memory photo format is not supported");
        }

        return outputStream.toByteArray();
    }

    private BufferedImage removeAlpha(BufferedImage image) {
        BufferedImage rgbImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgbImage.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }

        return rgbImage;
    }

    private void validate(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Memory photo is required");
        }
        if (photo.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Memory photo is too large");
        }
        if (!StringUtils.hasText(photo.getContentType()) || !StringUtils.hasText(photo.getOriginalFilename())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Memory photo must have a content type and original filename");
        }
        if (!StringUtils.hasText(StringUtils.getFilenameExtension(photo.getOriginalFilename()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Memory photo must have an allowed extension");
        }
    }

    private String resolveExtension(String originalFileName, String contentType) {
        String extension = StringUtils.getFilenameExtension(originalFileName);
        String normalizedExtension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        String normalizedContentType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);

        if (!ALLOWED_IMAGE_EXTENSIONS.contains(normalizedExtension)
                || !ALLOWED_IMAGE_CONTENT_TYPES.contains(normalizedContentType)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only jpg, jpeg, and png memory photos are allowed"
            );
        }

        return normalizedExtension;
    }

    private String normalizeFormatName(String extension) {
        if ("jpeg".equals(extension)) {
            return "jpg";
        }

        return extension;
    }
}
