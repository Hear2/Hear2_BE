package com.hear2.whatif.service;

import com.hear2.whatif.support.WhatIfCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class WhatIfPromptBuilder {

    private final ObjectMapper objectMapper;

    public String build(String question, WhatIfCategory category, WhatIfContext context) {
        try {
            return """
                    You are a relationship scenario assistant for a couple app.
                    Use only the summarized couple data below. Do not over-quote chat messages.

                    User question:
                    %s

                    Category:
                    %s

                    Summarized couple context:
                    %s

                    Rules:
                    - Write every user-facing value in Korean.
                    - Describe possible scenarios, not certain predictions.
                    - Do not expose sensitive personal data or raw chat history.
                    - Do not judge either partner as a fixed personality.
                    - Do not encourage breakup, escalation, medical, legal, or financial advice.
                    - Keep advice concrete and actionable.
                    - conflictRiskPercent must be an integer from 0 to 100.
                    - Return JSON only, without markdown.

                    Return exactly this JSON shape:
                    {
                      "scenarioSummary": "string",
                      "conflictRiskPercent": 0,
                      "riskFactors": ["string"],
                      "expectedReaction": "string",
                      "advice": "string",
                      "recommendedActions": ["string"]
                    }
                    """.formatted(
                    question,
                    category.name(),
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context.promptData())
            );
        } catch (Exception ex) {
            throw new IllegalStateException("failed to build what-if prompt", ex);
        }
    }
}
