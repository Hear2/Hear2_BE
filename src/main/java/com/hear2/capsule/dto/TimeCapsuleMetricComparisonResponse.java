package com.hear2.capsule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TimeCapsuleMetricComparisonResponse {

    @Schema(description = "봉인 당시 값", example = "100일")
    private String then;

    @Schema(description = "현재 값", example = "365일")
    private String now;

    @Schema(description = "변화량", example = "+265일")
    private String delta;
}
