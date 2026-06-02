package com.hear2.judge.dto;

import lombok.Builder;

@Builder
public record JudgeFeedbackSummary(
        boolean feedbackSubmitted,
        Boolean satisfied,
        String feedbackText
) {

    public static JudgeFeedbackSummary empty() {
        return new JudgeFeedbackSummary(false, null, null);
    }
}
