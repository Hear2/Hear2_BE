package com.hear2.judge.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum JudgeTone {
    WITTY,
    SERIOUS,
    SOFT;

    @JsonCreator
    public static JudgeTone from(String value) {
        if (value == null || value.isBlank()) {
            return WITTY;
        }

        String normalized = value.trim()
                .replace("-", "_")
                .replace(" ", "_")
                .toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "SERIOUS", "진지" -> SERIOUS;
            case "SOFT", "WARM", "부드러움", "따뜻함" -> SOFT;
            default -> WITTY;
        };
    }
}
