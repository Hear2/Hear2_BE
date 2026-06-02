package com.hear2.judge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "AI 판사 피드백 저장 응답")
public class JudgeFeedbackResponse {

    @Schema(description = "피드백 대상 판결 이력 ID", example = "123")
    private Long judgeHistoryId;

    @Schema(description = "현재 사용자의 만족 여부. 네=true, 아니요=false", example = "true")
    private Boolean satisfied;
}
