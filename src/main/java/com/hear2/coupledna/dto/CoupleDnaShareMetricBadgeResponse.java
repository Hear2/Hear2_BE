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
@Schema(description = "커플 DNA 공유 카드 상단 배지")
public class CoupleDnaShareMetricBadgeResponse {

    @Schema(description = "배지 라벨", example = "감성 89%")
    private String label;

    @Schema(description = "원본 점수", example = "89")
    private int score;
}
