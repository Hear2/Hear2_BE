package com.hear2.judge.support;

import com.hear2.emotion.enums.RiskLevel;

public final class JudgeTriggerPolicy {

    private static final double NEGATIVE_SCORE_THRESHOLD = 0.70;

    private JudgeTriggerPolicy() {
    }

    public static boolean isJudgeAvailable(RiskLevel riskLevel, Double negativeScore) {
        return isRiskLevelTrigger(riskLevel) || isNegativeScoreTrigger(negativeScore);
    }

    private static boolean isRiskLevelTrigger(RiskLevel riskLevel) {
        return riskLevel != null && riskLevel.getSeverity() >= RiskLevel.WARNING.getSeverity();
    }

    private static boolean isNegativeScoreTrigger(Double negativeScore) {
        return negativeScore != null && negativeScore >= NEGATIVE_SCORE_THRESHOLD;
    }
}
