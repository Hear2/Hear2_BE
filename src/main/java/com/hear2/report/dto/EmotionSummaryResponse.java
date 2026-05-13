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
@Schema(description = "감정 요약 정보")
public class EmotionSummaryResponse {

    @Schema(description = "감정 타입", example = "HAPPY")
    private String type;

    @Schema(description = "감정 라벨", example = "행복")
    private String label;

    @Schema(description = "감정 이모지", example = "😊")
    private String emoji;
}
