package com.hear2.memory.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Slf4j
@Service
public class MemoryPhotoMetadataExtractor {

    private static final int COORDINATE_SCALE = 7;

    public ExtractedMemoryPhotoMetadata extract(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            return empty();
        }

        try {
            Metadata metadata = ImageMetadataReader.readMetadata(photo.getInputStream());
            return new ExtractedMemoryPhotoMetadata(
                    extractTakenAt(metadata),
                    extractLatitude(metadata),
                    extractLongitude(metadata)
            );
        } catch (ImageProcessingException | IOException | RuntimeException e) {
            log.warn("Failed to extract memory photo metadata: {}", e.getMessage());
            return empty();
        }
    }

    private LocalDateTime extractTakenAt(Metadata metadata) {
        ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if (directory == null) {
            return null;
        }

        Date date = directory.getDateOriginal();
        if (date == null) {
            return null;
        }

        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    private BigDecimal extractLatitude(Metadata metadata) {
        GeoLocation geoLocation = extractGeoLocation(metadata);
        if (geoLocation == null) {
            return null;
        }

        return toCoordinate(geoLocation.getLatitude());
    }

    private BigDecimal extractLongitude(Metadata metadata) {
        GeoLocation geoLocation = extractGeoLocation(metadata);
        if (geoLocation == null) {
            return null;
        }

        return toCoordinate(geoLocation.getLongitude());
    }

    private GeoLocation extractGeoLocation(Metadata metadata) {
        GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
        if (gpsDirectory == null) {
            return null;
        }

        GeoLocation geoLocation = gpsDirectory.getGeoLocation();
        if (geoLocation == null || geoLocation.isZero()) {
            return null;
        }

        return geoLocation;
    }

    private BigDecimal toCoordinate(double coordinate) {
        return BigDecimal.valueOf(coordinate).setScale(COORDINATE_SCALE, RoundingMode.HALF_UP);
    }

    private ExtractedMemoryPhotoMetadata empty() {
        return new ExtractedMemoryPhotoMetadata(null, null, null);
    }
}
