package com.hear2.whatif.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WhatIfAiResponseParser {

    private static final String DEFAULT_SCENARIO = "현재 데이터만으로는 단정하기 어렵지만, 가능한 상황을 조심스럽게 살펴볼 수 있습니다.";
    private static final String DEFAULT_REACTION = "두 사람의 최근 대화와 감정 흐름에 따라 반응이 달라질 수 있습니다.";
    private static final String DEFAULT_ADVICE = "상대의 의도를 단정하지 말고, 기대하는 점을 짧고 구체적으로 먼저 나누는 것이 좋습니다.";

    private final ObjectMapper objectMapper;

    public WhatIfAiContent parseOrFallback(String outputText) {
        if (!StringUtils.hasText(outputText)) {
            return fallback();
        }

        try {
            WhatIfAiContent parsed = objectMapper.readValue(stripCodeFence(outputText), WhatIfAiContent.class);
            return normalize(parsed);
        } catch (Exception ignored) {
            return fallback();
        }
    }

    public WhatIfAiContent normalize(WhatIfAiContent content) {
        if (content == null) {
            return fallback();
        }

        return WhatIfAiContent.builder()
                .scenarioSummary(defaultIfBlank(content.getScenarioSummary(), DEFAULT_SCENARIO))
                .conflictRiskPercent(clamp(content.getConflictRiskPercent()))
                .riskFactors(nullSafeList(content.getRiskFactors()))
                .expectedReaction(defaultIfBlank(content.getExpectedReaction(), DEFAULT_REACTION))
                .advice(defaultIfBlank(content.getAdvice(), DEFAULT_ADVICE))
                .recommendedActions(nullSafeList(content.getRecommendedActions()))
                .build();
    }

    public WhatIfAiContent fallback() {
        return WhatIfAiContent.builder()
                .scenarioSummary(DEFAULT_SCENARIO)
                .conflictRiskPercent(0)
                .riskFactors(List.of())
                .expectedReaction(DEFAULT_REACTION)
                .advice(DEFAULT_ADVICE)
                .recommendedActions(List.of())
                .build();
    }

    private String stripCodeFence(String outputText) {
        String trimmed = outputText.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }

        int firstLineEnd = trimmed.indexOf('\n');
        int lastFenceStart = trimmed.lastIndexOf("```");
        if (firstLineEnd < 0 || lastFenceStart <= firstLineEnd) {
            return trimmed;
        }
        return trimmed.substring(firstLineEnd + 1, lastFenceStart).trim();
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private int clamp(Integer value) {
        if (value == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, value));
    }

    private List<String> nullSafeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
    }
}
