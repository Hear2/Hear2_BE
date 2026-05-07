package com.hear2.emotion.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.hear2.emotion.enums.EmotionType;
import com.hear2.emotion.enums.RiskLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "감정 분석 응답")
public class EmotionAnalysisResponse {

    @Schema(description = "감정 유형", example = "ANGRY", allowableValues = {"HAPPY", "SAD", "ANGRY", "ANXIOUS", "NEUTRAL"})
    @JsonAlias("emotion_type")
    private EmotionType emotionType;

    @Schema(description = "감정 점수. 0.0부터 1.0까지의 값입니다.", example = "0.82")
    @JsonAlias({"emotion_score", "score"})
    private Double emotionScore;

    @Schema(description = "부정 감정 점수. 0.0부터 1.0까지의 값입니다.", example = "0.82")
    @JsonAlias("negative_score")
    private Double negativeScore;

    @Schema(description = "말풍선 옆에 표시할 감정 이모지", example = "😡")
    @JsonAlias({"emotion_emoji", "emoji"})
    private String emotionEmoji;

    @Schema(description = "리스크 단계", example = "WARNING", allowableValues = {"NONE", "CAUTION", "WARNING", "DANGER"})
    @JsonAlias("risk_level")
    private RiskLevel riskLevel;

    @Schema(description = "리스크 감지 여부", example = "true")
    @JsonAlias("risk_detected")
    private Boolean riskDetected;

    @Schema(description = "리스크 감지 사유", example = "negative emotion score threshold exceeded")
    @JsonAlias({"risk_reason", "reason"})
    private String riskReason;

    @Schema(description = "감지된 위험 키워드 목록", example = "[\"미워\"]")
    @JsonAlias({"detected_risk_keywords", "risk_keywords"})
    private List<String> detectedRiskKeywords;
}
