package com.hear2.calendar.dto;

import lombok.Builder;

@Builder
public record GoogleCalendarCallbackResponse(
        boolean connected,
        Long userId,
        String calendarId
) {
}
