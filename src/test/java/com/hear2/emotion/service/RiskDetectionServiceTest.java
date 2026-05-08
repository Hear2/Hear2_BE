package com.hear2.emotion.service;

import com.hear2.emotion.dto.EmotionAnalysisResponse;
import com.hear2.emotion.dto.RiskAnalysisResponse;
import com.hear2.emotion.enums.EmotionType;
import com.hear2.emotion.enums.RiskLevel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RiskDetectionServiceTest {

    private final RiskDetectionService riskDetectionService = new RiskDetectionService();

    @Test
    void followsGptRiskLevelBeforeNegativeScore() {
        EmotionAnalysisResponse emotion = EmotionAnalysisResponse.builder()
                .emotionType(EmotionType.ANGRY)
                .emotionScore(0.95)
                .negativeScore(0.95)
                .riskLevel(RiskLevel.NONE)
                .build();

        RiskAnalysisResponse response = riskDetectionService.analyze("오늘은 감정이 복잡했어", emotion);

        assertThat(response.getRiskDetected()).isFalse();
        assertThat(response.getRiskLevel()).isEqualTo(RiskLevel.NONE);
    }

    @Test
    void adjustsRiskByOnlyOneLevelForSupportingKeyword() {
        EmotionAnalysisResponse emotion = EmotionAnalysisResponse.builder()
                .emotionType(EmotionType.ANGRY)
                .emotionScore(0.6)
                .negativeScore(0.6)
                .riskLevel(RiskLevel.NONE)
                .build();

        RiskAnalysisResponse response = riskDetectionService.analyze("너 때문에 너무 화나", emotion);

        assertThat(response.getRiskDetected()).isTrue();
        assertThat(response.getRiskLevel()).isEqualTo(RiskLevel.CAUTION);
        assertThat(response.getDetectedRiskKeywords()).contains("화나");
    }

    @Test
    void doesNotRaiseWarningToDangerForSupportingKeyword() {
        EmotionAnalysisResponse emotion = EmotionAnalysisResponse.builder()
                .emotionType(EmotionType.ANGRY)
                .emotionScore(0.85)
                .negativeScore(0.85)
                .riskLevel(RiskLevel.WARNING)
                .build();

        RiskAnalysisResponse response = riskDetectionService.analyze("진짜 미워", emotion);

        assertThat(response.getRiskDetected()).isTrue();
        assertThat(response.getRiskLevel()).isEqualTo(RiskLevel.WARNING);
    }

    @Test
    void detectsDangerOnlyForExplicitDangerKeyword() {
        EmotionAnalysisResponse emotion = EmotionAnalysisResponse.builder()
                .emotionType(EmotionType.NEUTRAL)
                .emotionScore(0.3)
                .negativeScore(0.0)
                .riskLevel(RiskLevel.NONE)
                .build();

        RiskAnalysisResponse response = riskDetectionService.analyze("가만 안 둬", emotion);

        assertThat(response.getRiskDetected()).isTrue();
        assertThat(response.getRiskLevel()).isEqualTo(RiskLevel.DANGER);
        assertThat(response.getDetectedRiskKeywords()).contains("가만 안 둬");
    }

    @Test
    void fallsBackToNegativeScoreWhenGptRiskLevelIsMissing() {
        EmotionAnalysisResponse emotion = EmotionAnalysisResponse.builder()
                .emotionType(EmotionType.ANGRY)
                .emotionScore(0.82)
                .negativeScore(0.82)
                .build();

        RiskAnalysisResponse response = riskDetectionService.analyze("오늘은 감정이 안 좋았어", emotion);

        assertThat(response.getRiskDetected()).isTrue();
        assertThat(response.getRiskLevel()).isEqualTo(RiskLevel.WARNING);
    }
}
