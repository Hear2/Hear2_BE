package com.hear2.capsule.dto;

import com.hear2.capsule.entity.TimeCapsuleCoverStyle;
import com.hear2.capsule.entity.TimeCapsuleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TimeCapsuleSummaryResponse {

    @Schema(description = "타임캡슐 ID", example = "1")
    private Long id;

    @Schema(description = "타임캡슐 이름", example = "1주년 기념 캡슐")
    private String name;

    @Schema(description = "타임캡슐 상태", example = "SEALED")
    private TimeCapsuleStatus status;

    @Schema(description = "커버 스타일", example = "LETTER")
    private TimeCapsuleCoverStyle coverStyle;

    @Schema(description = "개봉 예정 시각. UTC 기준으로 저장됩니다.", example = "2027-05-16T00:00:00")
    private LocalDateTime openAt;

    @Schema(description = "봉인 시각. UTC 기준으로 저장됩니다.", example = "2026-05-13T14:10:00")
    private LocalDateTime sealedAt;

    @Schema(description = "실제 개봉 처리 시각. 아직 열리지 않았으면 null", nullable = true)
    private LocalDateTime openedAt;

    @Schema(description = "개봉일까지 남은 일수. 열린 캡슐은 0", example = "365")
    private long dDay;
}
