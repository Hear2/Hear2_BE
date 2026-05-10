package com.hear2.location.dto;

import com.hear2.location.entity.UserLocation;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Getter
@Builder
public class LocationResponse {

    private Long coupleId;
    private Long userId;
    private Double latitude;
    private Double longitude;
    private Double accuracyMeters;
    private String locationName;
    private String placeName;
    private String addressName;
    private LocalDateTime recordedAt;
    private LocalDateTime updatedAt;

    public static LocationResponse from(UserLocation location) {
        return LocationResponse.builder()
                .coupleId(location.getCoupleId())
                .userId(location.getUserId())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .accuracyMeters(location.getAccuracyMeters())
                .locationName(resolveLocationName(location))
                .placeName(location.getPlaceName())
                .addressName(location.getAddressName())
                .recordedAt(location.getRecordedAt())
                .updatedAt(location.getUpdatedAt())
                .build();
    }

    private static String resolveLocationName(UserLocation location) {
        if (StringUtils.hasText(location.getPlaceName())) {
            return location.getPlaceName();
        }

        return StringUtils.hasText(location.getAddressName()) ? location.getAddressName() : null;
    }
}
