package com.hear2.calendar.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record GoogleCalendarConnectionStatusResponse(
        boolean connected,
        String calendarId,
        String scopes,
        LocalDateTime connectedAt,
        LocalDateTime updatedAt
) {

    public static GoogleCalendarConnectionStatusResponse disconnected() {
        return GoogleCalendarConnectionStatusResponse.builder()
                .connected(false)
                .build();
    }
}
