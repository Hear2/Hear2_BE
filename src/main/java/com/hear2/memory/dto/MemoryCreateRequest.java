package com.hear2.memory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Schema(
        description = "추억 생성 요청. 커플 ID와 업로더 ID는 로그인 토큰 기준으로 자동 적용됩니다.",
        example = """
                {
                  "memo": "명지대에서 찍은 사진",
                  "takenAt": "2026-05-11T10:30:00",
                  "latitude": 37.2221,
                  "longitude": 127.1875,
                  "locationName": "명지대학교 자연캠퍼스"
                }
                """
)
public class MemoryCreateRequest {

    @Schema(description = "추억 메모", example = "명지대에서 찍은 사진")
    @Size(max = 1000)
    private String memo;

    @Schema(description = "촬영 시간. 생략하면 사진 EXIF 촬영 시간 또는 현재 날짜 기준으로 처리됩니다.", example = "2026-05-11T10:30:00")
    private LocalDateTime takenAt;

    @Schema(description = "촬영 위도. 생략하면 사진 EXIF GPS 값을 사용합니다.", example = "37.2221")
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private BigDecimal latitude;

    @Schema(description = "촬영 경도. 생략하면 사진 EXIF GPS 값을 사용합니다.", example = "127.1875")
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private BigDecimal longitude;

    @Schema(description = "사용자가 직접 지정한 장소명. 생략하면 좌표 기준 카카오 로컬 API로 장소/주소를 계산합니다.", example = "명지대학교 자연캠퍼스")
    @Size(max = 255)
    private String locationName;
}
