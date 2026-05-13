package com.hear2.location.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        description = "내 현재 위치 업로드 요청. userId와 coupleId는 JWT 기준으로 자동 적용됩니다.",
        example = """
                {
                  "lat": 37.5641,
                  "lng": 126.9244,
                  "accuracy": 12.5,
                  "capturedAt": "2026-05-11T11:42:13Z"
                }
                """
)
public class LocationUpdateRequest {

    @NotNull
    @Schema(description = "위도", example = "37.5641")
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private Double lat;

    @NotNull
    @Schema(description = "경도", example = "126.9244")
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private Double lng;

    @Schema(description = "위치 정확도. 미터 단위", example = "12.5")
    @PositiveOrZero
    private Double accuracy;

    @Schema(description = "클라이언트가 위치를 캡처한 시각. UTC ISO8601 권장", example = "2026-05-11T11:42:13Z")
    private OffsetDateTime capturedAt;
}
