package com.hear2.calendar.dto;

import lombok.Builder;

@Builder
public record GoogleCalendarImportResponse(
        int importedCount,
        int updatedCount,
        int skippedCount
) {
}
