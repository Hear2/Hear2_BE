package com.hear2.calendar.dto;

import lombok.Builder;

@Builder
public record GoogleCalendarConnectResponse(
        String authorizationUrl,
        String state,
        int expiresInMinutes
) {
}
