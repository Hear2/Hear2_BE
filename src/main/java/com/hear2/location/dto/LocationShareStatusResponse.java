package com.hear2.location.dto;

import com.hear2.location.entity.LocationShareSetting;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class LocationShareStatusResponse {

    private Long coupleId;
    private Long userId;
    private boolean enabled;
    private LocalDateTime updatedAt;

    public static LocationShareStatusResponse from(LocationShareSetting setting) {
        return LocationShareStatusResponse.builder()
                .coupleId(setting.getCoupleId())
                .userId(setting.getUserId())
                .enabled(setting.isEnabled())
                .updatedAt(setting.getUpdatedAt())
                .build();
    }

    public static LocationShareStatusResponse disabled(Long coupleId, Long userId) {
        return LocationShareStatusResponse.builder()
                .coupleId(coupleId)
                .userId(userId)
                .enabled(false)
                .updatedAt(null)
                .build();
    }
}
