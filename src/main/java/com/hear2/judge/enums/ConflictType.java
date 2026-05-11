package com.hear2.judge.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum ConflictType {
    COMMUNICATION,
    JEALOUSY,
    TRUST,
    REPLY_DELAY,
    DAILY,
    OTHER;

    @JsonCreator
    public static ConflictType from(String value) {
        if (value == null || value.isBlank()) {
            return OTHER;
        }

        String normalized = value.trim()
                .replace("-", "_")
                .replace(" ", "_")
                .toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "COMMUNICATION", "대화", "소통" -> COMMUNICATION;
            case "JEALOUSY", "질투" -> JEALOUSY;
            case "TRUST", "신뢰" -> TRUST;
            case "REPLY_DELAY", "REPLY", "답장", "답장지연", "답장_지연" -> REPLY_DELAY;
            case "DAILY", "DAILY_LIFE", "일상" -> DAILY;
            default -> OTHER;
        };
    }
}
