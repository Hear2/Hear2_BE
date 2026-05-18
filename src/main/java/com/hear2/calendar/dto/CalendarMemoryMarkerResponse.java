package com.hear2.calendar.dto;

import com.hear2.memory.dto.MemoryCalendarDayResponse;
import lombok.Builder;

import java.util.List;

@Builder
public record CalendarMemoryMarkerResponse(
        boolean hasMemory,
        int memoryCount,
        List<String> thumbnails,
        String markerIcon
) {

    public static CalendarMemoryMarkerResponse empty() {
        return CalendarMemoryMarkerResponse.builder()
                .hasMemory(false)
                .memoryCount(0)
                .thumbnails(List.of())
                .markerIcon(null)
                .build();
    }

    public static CalendarMemoryMarkerResponse from(MemoryCalendarDayResponse day) {
        if (day == null) {
            return empty();
        }

        return CalendarMemoryMarkerResponse.builder()
                .hasMemory(day.getMemoryCount() > 0)
                .memoryCount(day.getMemoryCount())
                .thumbnails(day.getThumbnails() == null ? List.of() : day.getThumbnails())
                .markerIcon(day.getMemoryCount() > 0 ? "HEART" : null)
                .build();
    }
}
