package com.hear2.calendar.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record CalendarMonthResponse(
        int year,
        int month,
        List<CalendarMonthDayResponse> days,
        CalendarLegendResponse legend
) {
}
