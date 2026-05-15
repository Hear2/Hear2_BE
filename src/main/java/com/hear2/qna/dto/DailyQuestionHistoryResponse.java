package com.hear2.qna.dto;

import java.util.List;

public record DailyQuestionHistoryResponse(
        List<DailyQuestionHistoryItemResponse> items
) {
}
