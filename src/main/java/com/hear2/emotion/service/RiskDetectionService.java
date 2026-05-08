package com.hear2.emotion.service;

import com.hear2.emotion.dto.EmotionAnalysisResponse;
import com.hear2.emotion.dto.RiskAnalysisResponse;
import com.hear2.emotion.enums.EmotionType;
import com.hear2.emotion.enums.RiskLevel;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RiskDetectionService {

    private static final double CAUTION_THRESHOLD = 0.65;
    private static final double WARNING_THRESHOLD = 0.80;
    private static final double DANGER_THRESHOLD = 0.90;

    private static final List<String> EXPLICIT_DANGER_KEYWORDS = List.of(
            "죽어", "죽일", "죽여", "죽고 싶", "죽고싶", "자살", "자해",
            "해칠", "해쳐", "폭행", "때릴", "때려", "패버릴", "가만 안 둬",
            "칼로", "칼 들고", "협박"
    );

    private static final List<String> SUPPORTING_RISK_KEYWORDS = List.of(
            "헤어져", "꺼져", "차단", "끝내자", "다신 연락", "증오", "미워",
            "짜증", "화나", "싫어", "실망", "불안", "속상", "서운", "답답", "무시"
    );

    public RiskAnalysisResponse analyze(String content, EmotionAnalysisResponse emotion) {
        KeywordRisk keywordRisk = findKeywordRisk(content);
        RiskDecision baseRisk = resolveBaseRisk(emotion);
        RiskLevel riskLevel = applyKeywordAdjustment(baseRisk.riskLevel(), keywordRisk);

        boolean riskDetected = riskLevel.isRisk();
        return RiskAnalysisResponse.builder()
                .riskLevel(riskLevel)
                .riskDetected(riskDetected)
                .riskReason(riskDetected ? buildReason(baseRisk, keywordRisk, riskLevel) : null)
                .detectedRiskKeywords(keywordRisk.keywords())
                .build();
    }

    private RiskDecision resolveBaseRisk(EmotionAnalysisResponse emotion) {
        if (emotion == null) {
            return new RiskDecision(RiskLevel.NONE, false, null);
        }

        if (emotion.getRiskLevel() != null) {
            return new RiskDecision(emotion.getRiskLevel(), false, emotion.getRiskReason());
        }

        return new RiskDecision(scoreRiskLevel(emotion), true, null);
    }

    private RiskLevel scoreRiskLevel(EmotionAnalysisResponse emotion) {
        if (emotion == null) {
            return RiskLevel.NONE;
        }

        double negativeScore = resolveNegativeScore(emotion);
        if (negativeScore >= DANGER_THRESHOLD) {
            return RiskLevel.DANGER;
        }
        if (negativeScore >= WARNING_THRESHOLD) {
            return RiskLevel.WARNING;
        }
        if (negativeScore >= CAUTION_THRESHOLD) {
            return RiskLevel.CAUTION;
        }
        return RiskLevel.NONE;
    }

    private double resolveNegativeScore(EmotionAnalysisResponse emotion) {
        if (emotion.getNegativeScore() != null) {
            return clamp(emotion.getNegativeScore());
        }

        EmotionType emotionType = emotion.getEmotionType();
        if (emotionType != null && emotionType.isNegative() && emotion.getEmotionScore() != null) {
            return clamp(emotion.getEmotionScore());
        }
        return 0.0;
    }

    private KeywordRisk findKeywordRisk(String content) {
        if (!StringUtils.hasText(content)) {
            return new KeywordRisk(false, false, List.of());
        }

        String normalized = content.toLowerCase(Locale.ROOT);
        Set<String> detectedKeywords = new LinkedHashSet<>();
        boolean explicitDangerDetected = false;
        boolean supportingRiskDetected = false;

        for (String keyword : EXPLICIT_DANGER_KEYWORDS) {
            if (containsKeyword(normalized, keyword)) {
                detectedKeywords.add(keyword);
                explicitDangerDetected = true;
            }
        }

        for (String keyword : SUPPORTING_RISK_KEYWORDS) {
            if (containsKeyword(normalized, keyword)) {
                detectedKeywords.add(keyword);
                supportingRiskDetected = true;
            }
        }

        return new KeywordRisk(explicitDangerDetected, supportingRiskDetected, List.copyOf(detectedKeywords));
    }

    private boolean containsKeyword(String normalizedContent, String keyword) {
        return normalizedContent.contains(keyword.toLowerCase(Locale.ROOT));
    }

    private RiskLevel applyKeywordAdjustment(RiskLevel baseRiskLevel, KeywordRisk keywordRisk) {
        if (keywordRisk.explicitDangerDetected()) {
            return RiskLevel.DANGER;
        }

        if (!keywordRisk.supportingRiskDetected()) {
            return baseRiskLevel;
        }

        return switch (baseRiskLevel) {
            case NONE -> RiskLevel.CAUTION;
            case CAUTION -> RiskLevel.WARNING;
            case WARNING, DANGER -> baseRiskLevel;
        };
    }

    private String buildReason(RiskDecision baseRisk, KeywordRisk keywordRisk, RiskLevel finalRiskLevel) {
        List<String> reasons = new ArrayList<>();

        if (baseRisk.riskLevel().isRisk()) {
            if (baseRisk.scoreFallback()) {
                reasons.add("negative emotion score fallback");
            } else if (StringUtils.hasText(baseRisk.reason())) {
                reasons.add("gpt risk judgment: " + baseRisk.reason());
            } else {
                reasons.add("gpt risk judgment");
            }
        }
        if (keywordRisk.explicitDangerDetected()) {
            reasons.add("explicit violence/self-harm/threat keyword detected");
        } else if (keywordRisk.supportingRiskDetected() && finalRiskLevel.isHigherThan(baseRisk.riskLevel())) {
            reasons.add("supporting keyword adjusted risk by one level");
        }
        if (reasons.isEmpty()) {
            reasons.add("risk detected");
        }

        return String.join(", ", reasons);
    }

    private double clamp(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }

    private record RiskDecision(RiskLevel riskLevel, boolean scoreFallback, String reason) {
    }

    private record KeywordRisk(
            boolean explicitDangerDetected,
            boolean supportingRiskDetected,
            List<String> keywords
    ) {
    }
}
