package com.hear2.memory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
@Schema(
        description = "3초 기록 수정 요청",
        example = """
                {
                  "note": "오늘도 행복한 하루",
                  "locationName": "명지대학교 자연캠퍼스",
                  "userTags": ["우리둘이"]
                }
                """
)
public class MemoryQuickUpdateRequest {

    @Size(max = 1000)
    @Schema(description = "한 줄 노트", example = "오늘도 행복한 하루")
    private String note;

    @Size(max = 255)
    @Schema(description = "사용자가 직접 수정한 장소명. null이면 기존 위치명을 유지합니다.", example = "명지대학교 자연캠퍼스")
    private String locationName;

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    @Schema(description = "수정된 위도. null이면 기존 위도를 유지합니다.", example = "37.2228")
    private BigDecimal lat;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    @Schema(description = "수정된 경도. null이면 기존 경도를 유지합니다.", example = "127.1875")
    private BigDecimal lng;

    @Size(max = 20)
    @Schema(description = "사용자 직접 입력 태그. null이면 기존 태그를 유지하고, []이면 모두 삭제합니다.", example = "[\"우리둘이\"]")
    private List<@Size(max = 80) String> userTags;
}
