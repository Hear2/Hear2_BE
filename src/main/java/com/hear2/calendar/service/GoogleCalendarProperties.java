package com.hear2.calendar.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google.calendar")
public record GoogleCalendarProperties(
        String redirectUri,
        String connectSuccessUrl,
        int oauthStateExpirationMinutes
) {
}
