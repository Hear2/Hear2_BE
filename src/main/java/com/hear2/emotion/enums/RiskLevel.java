package com.hear2.emotion.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum RiskLevel {
    NONE(0, "없음"),
    CAUTION(1, "주의"),
    WARNING(2, "경고"),
    DANGER(3, "위험");

    private final int severity;
    private final String displayName;

    public boolean isRisk() {
        return this != NONE;
    }

    public boolean isHigherThan(RiskLevel other) {
        return this.severity > other.severity;
    }

    @JsonCreator
    public static RiskLevel from(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }

        String normalized = value.trim()
                .replace("-", "_")
                .replace(" ", "_")
                .toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "CAUTION", "LOW", "주의" -> CAUTION;
            case "WARNING", "MEDIUM", "WARN", "경고" -> WARNING;
            case "DANGER", "HIGH", "CRITICAL", "위험" -> DANGER;
            default -> NONE;
        };
    }
}
