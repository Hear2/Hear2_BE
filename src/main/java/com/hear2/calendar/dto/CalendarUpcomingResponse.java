package com.hear2.calendar.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record CalendarUpcomingResponse(
        List<CalendarEventSummaryResponse> events
) {
}
