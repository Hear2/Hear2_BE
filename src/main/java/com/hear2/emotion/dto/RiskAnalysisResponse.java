package com.hear2.emotion.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
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
@Schema(description = "리스크 분석 응답")
public class RiskAnalysisResponse {

    @Schema(description = "리스크 단계", example = "DANGER", allowableValues = {"NONE", "CAUTION", "WARNING", "DANGER"})
    @JsonAlias("risk_level")
    private RiskLevel riskLevel;

    @Schema(description = "리스크 감지 여부", example = "true")
    @JsonAlias("risk_detected")
    private Boolean riskDetected;

    @Schema(description = "리스크 감지 사유", example = "risk keyword detected")
    @JsonAlias({"risk_reason", "reason"})
    private String riskReason;

    @Schema(description = "감지된 위험 키워드 목록", example = "[\"가만 안 둬\"]")
    @JsonAlias({"detected_risk_keywords", "risk_keywords"})
    private List<String> detectedRiskKeywords;
}
