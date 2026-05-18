package com.hear2.calendar.dto;

import com.hear2.calendar.entity.CalendarEvent;
import com.hear2.calendar.entity.CalendarEventViewType;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Builder
public record CalendarEventSummaryResponse(
        Long id,
        String title,
        CalendarEventViewType viewType,
        CalendarEventColor color,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        boolean allDay
) {

    public static CalendarEventSummaryResponse from(CalendarEvent event, CalendarEventViewType viewType) {
        return CalendarEventSummaryResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .viewType(viewType)
                .color(CalendarEventColor.from(viewType))
                .startsAt(toUtc(event.getStartsAt()))
                .endsAt(toUtc(event.getEndsAt()))
                .allDay(event.isAllDay())
                .build();
    }

    private static OffsetDateTime toUtc(java.time.LocalDateTime dateTime) {
        return dateTime == null ? null : OffsetDateTime.of(dateTime, ZoneOffset.UTC);
    }
}
