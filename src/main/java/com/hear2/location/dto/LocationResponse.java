package com.hear2.location.dto;

import com.hear2.location.entity.UserLocation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "사용자 위치 정보")
public class LocationResponse {

    @Schema(description = "위치가 속한 커플 ID", example = "2")
    private Long coupleId;

    @Schema(description = "위치 소유 사용자 ID", example = "7")
    private Long userId;

    @Schema(description = "위도", example = "37.2221")
    private Double lat;

    @Schema(description = "경도", example = "127.1875")
    private Double lng;

    @Schema(description = "프론트 위치 측정 정확도, 미터 단위", example = "20.0")
    private Double accuracy;

    @Schema(description = "프론트 표시용 위치명. 장소명이 있으면 장소명, 없으면 주소명입니다.", example = "명지대학교 자연캠퍼스")
    private String locationName;

    @Schema(description = "카카오 Local API로 변환한 장소명", example = "명지대학교 자연캠퍼스")
    private String placeName;

    @Schema(description = "카카오 Local API로 변환한 주소명", example = "경기 용인시 처인구 명지로 116")
    private String addressName;

    @Schema(description = "프론트가 위치를 획득한 시각")
    private LocalDateTime capturedAt;

    @Schema(description = "서버에 마지막으로 저장된 시각")
    private LocalDateTime updatedAt;

    @Schema(description = "마지막 업데이트가 5분보다 오래되었는지 여부", example = "false")
    private boolean stale;

    public static LocationResponse from(UserLocation location, boolean stale) {
        return LocationResponse.builder()
                .coupleId(location.getCoupleId())
                .userId(location.getUserId())
                .lat(location.getLatitude())
                .lng(location.getLongitude())
                .accuracy(location.getAccuracyMeters())
                .locationName(resolveLocationName(location))
                .placeName(location.getPlaceName())
                .addressName(location.getAddressName())
                .capturedAt(location.getRecordedAt())
                .updatedAt(location.getUpdatedAt())
                .stale(stale)
                .build();
    }

    private static String resolveLocationName(UserLocation location) {
        if (StringUtils.hasText(location.getPlaceName())) {
            return location.getPlaceName();
        }

        return StringUtils.hasText(location.getAddressName()) ? location.getAddressName() : null;
    }
}
