package com.hear2.calendar.dto;

import lombok.Builder;

@Builder
public record GoogleCalendarSyncResponse(
        Long eventId,
        String googleEventId,
        boolean synced
) {
}
