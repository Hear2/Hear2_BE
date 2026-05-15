package com.hear2.capsule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TimeCapsuleThenVsNowResponse {

    @Schema(description = "봉인 당시와 현재의 함께한 일수 비교")
    private TimeCapsuleMetricComparisonResponse daysTogether;

    @Schema(description = "봉인 당시와 현재의 메시지 수 비교")
    private TimeCapsuleMetricComparisonResponse messages;

    @Schema(description = "봉인 당시와 현재의 추억 사진 수 비교")
    private TimeCapsuleMetricComparisonResponse photos;

    @Schema(description = "봉인 당시와 현재의 캐릭터 레벨 비교")
    private TimeCapsuleMetricComparisonResponse characterLevel;
}
