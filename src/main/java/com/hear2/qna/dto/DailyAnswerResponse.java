package com.hear2.qna.dto;

public record DailyAnswerResponse(
        boolean ok,
        boolean bothAnswered,
        int streak,
        int earnedPoints
) {
}
