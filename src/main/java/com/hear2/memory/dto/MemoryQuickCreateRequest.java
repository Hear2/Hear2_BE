package com.hear2.memory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@Schema(
        description = "3초 기록 생성 요청. objectKey는 /media/presigned-url 발급 응답의 값입니다. 기존 호환을 위해 imageUrl도 받을 수 있습니다.",
        example = """
                {
                  "objectKey": "media/memory/7/20260513/9d2c.jpg",
                  "lat": 37.5641,
                  "lng": 126.9244,
                  "capturedAt": "2026-05-12T14:00:00Z",
                  "userTags": ["우리둘이", "특별한날"]
                }
                """
)
public class MemoryQuickCreateRequest {

    @Size(max = 2048)
    @Schema(description = "기존 호환용 이미지 URL. 가능하면 objectKey를 사용하세요.", example = "https://cdn.example.com/memories/2/photo.jpg")
    private String imageUrl;

    @Size(max = 2048)
    @Schema(description = "presigned URL 업로드 후 받은 R2/S3 object key", example = "media/memory/7/20260513/9d2c.jpg")
    private String objectKey;

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    @Schema(description = "촬영 위도. 이미지 EXIF가 없을 때 사용합니다.", example = "37.5641")
    private BigDecimal lat;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    @Schema(description = "촬영 경도. 이미지 EXIF가 없을 때 사용합니다.", example = "126.9244")
    private BigDecimal lng;

    @Schema(description = "촬영 시각. UTC ISO8601 권장", example = "2026-05-12T14:00:00Z")
    private OffsetDateTime capturedAt;

    @Size(max = 20)
    @Schema(description = "사용자 직접 입력 태그. #은 생략해도 됩니다.", example = "[\"우리둘이\", \"특별한날\"]")
    private List<@Size(max = 80) String> userTags;
}
