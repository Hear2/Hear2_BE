package com.hear2.calendar.dto;

import com.hear2.calendar.entity.CalendarEventViewType;

public record CalendarEventColor(
        CalendarEventViewType type,
        String name,
        String hex
) {

    public static CalendarEventColor from(CalendarEventViewType type) {
        return switch (type) {
            case OWNER -> new CalendarEventColor(type, "Orange", "#FFB28A");
            case PARTNER -> new CalendarEventColor(type, "Blue", "#9CCBFF");
            case SHARED -> new CalendarEventColor(type, "Pink", "#F7A6C8");
        };
    }
}
