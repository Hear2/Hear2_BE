package com.hear2.qna.dto;

public record DailyQuestionHistoryItemResponse(
        long day,
        Long questionId,
        String question,
        DailyQuestionStatus status
) {
}
