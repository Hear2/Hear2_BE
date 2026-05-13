package com.hear2.coupledna.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "커플 DNA 지표 항목")
public class CoupleDnaMetricResponse {

    @Schema(description = "지표 라벨", example = "감성소통")
    private String label;

    @Schema(description = "지표 점수", example = "89")
    private int score;
}
