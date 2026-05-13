package com.hear2.location.dto;

import com.hear2.location.entity.LocationShareSetting;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "내 위치 공유 상태 응답")
public class LocationShareStatusResponse {

    @Schema(description = "JWT 사용자에게 연결된 커플 ID", example = "2")
    private Long coupleId;

    @Schema(description = "JWT에서 추출한 로그인 사용자 ID", example = "7")
    private Long userId;

    @Schema(description = "위치 공유 활성화 여부", example = "true")
    private boolean enabled;

    @Schema(description = "공유 상태가 마지막으로 변경된 시각")
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
