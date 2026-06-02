package com.hear2.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "감정 분석 결과 피드백 요청")
public class EmotionFeedbackRequest {

    @NotNull
    @Schema(description = "감정 분석 결과가 맞는지 여부. 맞아요=true, 아니에요=false", example = "true")
    private Boolean isCorrect;
}
