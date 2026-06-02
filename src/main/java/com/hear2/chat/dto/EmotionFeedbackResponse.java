package com.hear2.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "감정 분석 결과 피드백 저장 응답")
public class EmotionFeedbackResponse {

    @Schema(description = "피드백 대상 메시지 ID", example = "1")
    private Long messageId;

    @Schema(description = "현재 사용자가 남긴 피드백. 맞아요=true, 아니에요=false", example = "true")
    private Boolean isCorrect;
}
