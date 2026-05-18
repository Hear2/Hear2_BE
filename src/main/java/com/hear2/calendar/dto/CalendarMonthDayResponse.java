package com.hear2.calendar.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record CalendarMonthDayResponse(
        LocalDate date,
        List<CalendarEventSummaryResponse> events,
        CalendarMemoryMarkerResponse memoryMarker
) {
}
