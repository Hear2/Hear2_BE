package com.hear2.whatif.service;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WhatIfAiResponseParserTest {

    private final WhatIfAiResponseParser parser = new WhatIfAiResponseParser(new ObjectMapper());

    @Test
    void normalizesMissingListsAndClampsRiskPercent() {
        WhatIfAiContent parsed = parser.parseOrFallback("""
                {
                  "scenarioSummary": "summary",
                  "conflictRiskPercent": 142,
                  "riskFactors": null,
                  "expectedReaction": "reaction",
                  "advice": "advice",
                  "recommendedActions": null
                }
                """);

        assertThat(parsed.getScenarioSummary()).isEqualTo("summary");
        assertThat(parsed.getConflictRiskPercent()).isEqualTo(100);
        assertThat(parsed.getRiskFactors()).isEmpty();
        assertThat(parsed.getRecommendedActions()).isEmpty();
    }

    @Test
    void returnsFallbackForInvalidJson() {
        WhatIfAiContent parsed = parser.parseOrFallback("not-json");

        assertThat(parsed.getConflictRiskPercent()).isZero();
        assertThat(parsed.getRiskFactors()).isEqualTo(List.of());
        assertThat(parsed.getScenarioSummary()).isNotBlank();
    }
}
