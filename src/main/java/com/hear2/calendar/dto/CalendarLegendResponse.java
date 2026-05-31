package com.hear2.calendar.dto;

import com.hear2.calendar.entity.CalendarEventViewType;
import lombok.Builder;

import java.util.List;

@Builder
public record CalendarLegendResponse(
        List<CalendarEventColor> eventColors,
        String memoryMarkerIcon
) {

    public static CalendarLegendResponse defaults() {
        return CalendarLegendResponse.builder()
                .eventColors(List.of(
                        CalendarEventColor.from(CalendarEventViewType.OWNER),
                        CalendarEventColor.from(CalendarEventViewType.PARTNER),
                        CalendarEventColor.from(CalendarEventViewType.SHARED)
                ))
                .memoryMarkerIcon("HEART")
                .build();
    }
}
