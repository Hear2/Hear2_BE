package com.hear2.character.dto;

import com.hear2.character.entity.CharacterExpHistory;
import com.hear2.character.support.CharacterExpSourceType;

import java.time.LocalDateTime;

public record CharacterExpHistoryItemResponse(
        CharacterExpSourceType sourceType,
        long expAmount,
        LocalDateTime createdAt
) {

    public static CharacterExpHistoryItemResponse from(CharacterExpHistory history) {
        return new CharacterExpHistoryItemResponse(
                history.getSourceType(),
                history.getExpAmount(),
                history.getCreatedAt()
        );
    }
}
