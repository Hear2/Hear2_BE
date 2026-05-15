package com.hear2.qna.dto;

import java.time.LocalDate;

public record DailyQuestionDetailResponse(
        long day,
        Long questionId,
        String question,
        LocalDate questionDate,
        String myAnswer,
        String partnerAnswer,
        boolean bothAnswered
) {
}
