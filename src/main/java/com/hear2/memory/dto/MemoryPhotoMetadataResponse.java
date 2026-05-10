package com.hear2.memory.dto;

import com.hear2.memory.entity.MemoryPhotoMetadata;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class MemoryPhotoMetadataResponse {

    private LocalDateTime takenAt;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String locationName;
    private String placeName;
    private String addressName;

    public static MemoryPhotoMetadataResponse from(MemoryPhotoMetadata metadata) {
        if (metadata == null) {
            return null;
        }

        return MemoryPhotoMetadataResponse.builder()
                .takenAt(metadata.getTakenAt())
                .latitude(metadata.getLatitude())
                .longitude(metadata.getLongitude())
                .locationName(metadata.getLocationName())
                .placeName(metadata.getPlaceName())
                .addressName(metadata.getAddressName())
                .build();
    }
}
