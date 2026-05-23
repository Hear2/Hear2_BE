package com.hear2.character.dto;

import java.util.List;

public record CharacterExpHistoryTodayResponse(
        long totalExp,
        List<CharacterExpHistoryItemResponse> items
) {

    public static CharacterExpHistoryTodayResponse from(List<CharacterExpHistoryItemResponse> items) {
        long totalExp = items.stream()
                .mapToLong(CharacterExpHistoryItemResponse::expAmount)
                .sum();

        return new CharacterExpHistoryTodayResponse(totalExp, items);
    }
}
