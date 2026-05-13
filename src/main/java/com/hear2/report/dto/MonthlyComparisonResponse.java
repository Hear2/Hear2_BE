package com.hear2.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "전월 대비 변화율")
public class MonthlyComparisonResponse {

    @Schema(description = "대화량 변화율", nullable = true, example = "12")
    private Integer conversationChangeRate;

    @Schema(description = "긍정 비율 변화율", nullable = true, example = "5")
    private Integer positiveRatioChangeRate;

    @Schema(description = "사진 기록 변화율", nullable = true, example = "24")
    private Integer photoChangeRate;

    @Schema(description = "갈등 발생 변화율. AI Judge history 기준", nullable = true, example = "-8")
    private Integer conflictChangeRate;
}
