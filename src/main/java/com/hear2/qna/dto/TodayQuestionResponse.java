package com.hear2.qna.dto;

import java.time.LocalDateTime;

public record TodayQuestionResponse(
        long day,
        Long questionId,
        String question,
        String myAnswer,
        LocalDateTime myAnsweredAt,
        boolean partnerAnswered,
        String partnerAnswer,
        boolean bothAnswered,
        int streak,
        int rewardPoints
) {
}
