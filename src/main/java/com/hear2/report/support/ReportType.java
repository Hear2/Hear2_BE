package com.hear2.report.support;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum ReportType {
    WEEKLY,
    MONTHLY;

    @JsonCreator
    public static ReportType from(String value) {
        if (value == null || value.isBlank()) {
            return WEEKLY;
        }

        String normalized = value.trim()
                .replace("-", "_")
                .replace(" ", "_")
                .toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "MONTH", "MONTHLY", "월간" -> MONTHLY;
            default -> WEEKLY;
        };
    }
}
