package com.hear2.emotion.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum EmotionType {
    HAPPY("행복", "😊", false),
    SAD("슬픔", "😢", true),
    ANGRY("분노", "😡", true),
    ANXIOUS("불안", "😟", true),
    NEUTRAL("중립", "😐", false);

    private final String displayName;
    private final String emoji;
    private final boolean negative;

    @JsonCreator
    public static EmotionType from(String value) {
        if (value == null || value.isBlank()) {
            return NEUTRAL;
        }

        String normalized = value.trim()
                .replace("-", "_")
                .replace(" ", "_")
                .toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "HAPPY", "JOY", "POSITIVE", "행복", "기쁨", "좋음" -> HAPPY;
            case "SAD", "SADNESS", "슬픔", "우울", "속상" -> SAD;
            case "ANGRY", "ANGER", "MAD", "분노", "화남", "짜증" -> ANGRY;
            case "ANXIOUS", "ANXIETY", "WORRIED", "불안", "걱정" -> ANXIOUS;
            default -> NEUTRAL;
        };
    }
}
