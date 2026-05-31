package com.hear2.calendar.service;

import com.hear2.calendar.dto.GoogleCalendarCallbackResponse;
import com.hear2.calendar.dto.GoogleCalendarConnectResponse;
import com.hear2.calendar.dto.GoogleCalendarConnectionStatusResponse;
import com.hear2.calendar.dto.GoogleCalendarImportResponse;
import com.hear2.calendar.dto.GoogleCalendarSyncResponse;
import com.hear2.calendar.entity.CalendarEvent;
import com.hear2.calendar.entity.CalendarEventVisibility;
import com.hear2.calendar.entity.CalendarExternalProvider;
import com.hear2.calendar.entity.GoogleCalendarConnection;
import com.hear2.calendar.entity.GoogleCalendarOAuthState;
import com.hear2.calendar.repository.CalendarEventRepository;
import com.hear2.calendar.repository.GoogleCalendarConnectionRepository;
import com.hear2.calendar.repository.GoogleCalendarOAuthStateRepository;
import com.hear2.couple.entity.CoupleMember;
import com.hear2.couple.repository.CoupleMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarService {

    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_CALENDAR_API_URL = "https://www.googleapis.com/calendar/v3";
    private static final String DEFAULT_CALENDAR_ID = "primary";
    private static final DateTimeFormatter RFC3339_UTC_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final String CALENDAR_SCOPES = String.join(" ",
            "https://www.googleapis.com/auth/calendar.events",
            "https://www.googleapis.com/auth/calendar.calendarlist.readonly"
    );

    private final GoogleCalendarProperties googleCalendarProperties;
    private final GoogleCalendarConnectionRepository googleCalendarConnectionRepository;
    private final GoogleCalendarOAuthStateRepository googleCalendarOAuthStateRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final CoupleMemberRepository coupleMemberRepository;

    private final RestClient restClient = RestClient.create();

    @Value("${google.oauth.client-id:}")
    private String clientId;

    @Value("${google.oauth.client-secret:}")
    private String clientSecret;

    @Transactional
    public GoogleCalendarConnectResponse createConnectUrl(Long currentUserId) {
        validateCurrentUserId(currentUserId);
        requireGoogleOAuthConfig();

        int expiresInMinutes = resolveStateExpirationMinutes();
        String state = UUID.randomUUID().toString();
        googleCalendarOAuthStateRepository.save(GoogleCalendarOAuthState.builder()
                .state(state)
                .userId(currentUserId)
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(expiresInMinutes))
                .used(false)
                .build());

        String authorizationUrl = UriComponentsBuilder.fromUriString(GOOGLE_AUTH_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", resolveRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", CALENDAR_SCOPES)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();

        return GoogleCalendarConnectResponse.builder()
                .authorizationUrl(authorizationUrl)
                .state(state)
                .expiresInMinutes(expiresInMinutes)
                .build();
    }

    @Transactional
    public GoogleCalendarCallbackResponse handleCallback(String code, String state) {
        requireGoogleOAuthConfig();
        if (!StringUtils.hasText(code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "code is required");
        }
        GoogleCalendarOAuthState oauthState = validateState(state);

        Map<?, ?> tokenResponse = requestToken(code);
        String accessToken = stringValue(tokenResponse.get("access_token"));
        String refreshToken = stringValue(tokenResponse.get("refresh_token"));
        String scopes = stringValue(tokenResponse.get("scope"));
        long expiresInSeconds = longValue(tokenResponse.get("expires_in"), 3600L);
        Long userId = oauthState.getUserId();
        GoogleCalendarConnection connection = googleCalendarConnectionRepository.findByUserId(userId)
                .orElseGet(() -> GoogleCalendarConnection.builder()
                        .userId(userId)
                        .calendarId(DEFAULT_CALENDAR_ID)
                        .build());
        if (!StringUtils.hasText(accessToken)
                || (!StringUtils.hasText(refreshToken) && !StringUtils.hasText(connection.getRefreshToken()))) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google did not return calendar tokens");
        }

        LocalDateTime expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(expiresInSeconds);
        connection.updateTokens(accessToken, refreshToken, expiresAt, resolveScopes(scopes));
        googleCalendarConnectionRepository.save(connection);
        oauthState.markUsed();

        return GoogleCalendarCallbackResponse.builder()
                .connected(true)
                .userId(userId)
                .calendarId(connection.getCalendarId())
                .build();
    }

    @Transactional(readOnly = true)
    public GoogleCalendarConnectionStatusResponse getStatus(Long currentUserId) {
        validateCurrentUserId(currentUserId);
        return googleCalendarConnectionRepository.findByUserId(currentUserId)
                .map(connection -> GoogleCalendarConnectionStatusResponse.builder()
                        .connected(true)
                        .calendarId(connection.getCalendarId())
                        .scopes(connection.getScopes())
                        .connectedAt(connection.getConnectedAt())
                        .updatedAt(connection.getUpdatedAt())
                        .build())
                .orElseGet(GoogleCalendarConnectionStatusResponse::disconnected);
    }

    @Transactional
    public void disconnect(Long currentUserId) {
        validateCurrentUserId(currentUserId);
        googleCalendarConnectionRepository.deleteByUserId(currentUserId);
    }

    @Transactional
    public GoogleCalendarSyncResponse syncEvent(Long eventId, Long currentUserId) {
        CoupleContext context = resolveCoupleContext(currentUserId);
        CalendarEvent event = calendarEventRepository.findByIdAndCoupleId(eventId, context.coupleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "calendar event not found"));
        String googleEventId = syncEvent(event, currentUserId, true);

        return GoogleCalendarSyncResponse.builder()
                .eventId(event.getId())
                .googleEventId(googleEventId)
                .synced(StringUtils.hasText(googleEventId))
                .build();
    }

    @Transactional
    public String syncEventIfConnected(CalendarEvent event, Long currentUserId) {
        return syncEvent(event, currentUserId, false);
    }

    private String syncEvent(CalendarEvent event, Long currentUserId, boolean failOnError) {
        if (event == null || currentUserId == null) {
            return null;
        }
        GoogleCalendarConnection connection = googleCalendarConnectionRepository.findByUserId(currentUserId)
                .orElse(null);
        if (connection == null || !isConfigured()) {
            return null;
        }

        try {
            GoogleCalendarConnection activeConnection = ensureFreshAccessToken(connection);
            Map<String, Object> requestBody = toGoogleEventRequest(event);
            Map<?, ?> response;
            if (event.getExternalProvider() == CalendarExternalProvider.GOOGLE
                    && StringUtils.hasText(event.getExternalEventId())) {
                response = restClient.patch()
                        .uri(GOOGLE_CALENDAR_API_URL + "/calendars/{calendarId}/events/{eventId}",
                                activeConnection.getCalendarId(),
                                event.getExternalEventId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + activeConnection.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(Map.class);
            } else {
                response = restClient.post()
                        .uri(GOOGLE_CALENDAR_API_URL + "/calendars/{calendarId}/events", activeConnection.getCalendarId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + activeConnection.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(Map.class);
            }

            String googleEventId = stringValue(response == null ? null : response.get("id"));
            if (StringUtils.hasText(googleEventId)) {
                event.markExternalSync(CalendarExternalProvider.GOOGLE, googleEventId, LocalDateTime.now(ZoneOffset.UTC));
            }
            return googleEventId;
        } catch (ResponseStatusException | RestClientException exception) {
            if (failOnError) {
                throw googleSyncFailed(exception);
            }
            log.warn("Google Calendar auto sync skipped. eventId={}, userId={}",
                    event.getId(),
                    currentUserId,
                    exception);
            return null;
        }
    }

    private ResponseStatusException googleSyncFailed(RuntimeException exception) {
        if (exception instanceof ResponseStatusException responseStatusException) {
            return responseStatusException;
        }
        if (exception instanceof RestClientResponseException responseException) {
            return new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    googleFailureMessage(responseException),
                    exception
            );
        }

        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Google Calendar sync failed",
                exception
        );
    }

    private String googleFailureMessage(RestClientResponseException exception) {
        return "Google Calendar request failed (HTTP %d): %s"
                .formatted(
                        exception.getStatusCode().value(),
                        abbreviateGoogleErrorBody(exception.getResponseBodyAsString())
                );
    }

    private String abbreviateGoogleErrorBody(String body) {
        if (!StringUtils.hasText(body)) {
            return "empty response";
        }

        String compactBody = body.replaceAll("\\s+", " ").trim();
        return compactBody.length() > 500 ? compactBody.substring(0, 500) + "..." : compactBody;
    }

    @Transactional
    public void deleteGoogleEventIfConnected(CalendarEvent event, Long currentUserId) {
        if (event == null
                || event.getExternalProvider() != CalendarExternalProvider.GOOGLE
                || !StringUtils.hasText(event.getExternalEventId())) {
            return;
        }
        GoogleCalendarConnection connection = googleCalendarConnectionRepository.findByUserId(currentUserId)
                .orElse(null);
        if (connection == null || !isConfigured()) {
            return;
        }

        try {
            GoogleCalendarConnection activeConnection = ensureFreshAccessToken(connection);
            restClient.delete()
                    .uri(GOOGLE_CALENDAR_API_URL + "/calendars/{calendarId}/events/{eventId}",
                            activeConnection.getCalendarId(),
                            event.getExternalEventId())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + activeConnection.getAccessToken())
                    .retrieve()
                    .toBodilessEntity();
        } catch (ResponseStatusException | RestClientException exception) {
            log.warn("Google Calendar delete sync skipped. eventId={}, userId={}",
                    event.getId(),
                    currentUserId,
                    exception);
        }
    }

    @Transactional
    public GoogleCalendarImportResponse importEvents(int year, int month, Long currentUserId) {
        validateCurrentUserId(currentUserId);
        CoupleContext context = resolveCoupleContext(currentUserId);
        GoogleCalendarConnection connection = googleCalendarConnectionRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google Calendar is not connected"));

        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.plusMonths(1);
        Map<?, ?> response;
        try {
            GoogleCalendarConnection activeConnection = ensureFreshAccessToken(connection);
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("www.googleapis.com")
                            .path("/calendar/v3/calendars/{calendarId}/events")
                            .queryParam("timeMin", toRfc3339Utc(monthStart.atStartOfDay()))
                            .queryParam("timeMax", toRfc3339Utc(monthEnd.atStartOfDay()))
                            .queryParam("singleEvents", "true")
                            .queryParam("orderBy", "startTime")
                            .build(activeConnection.getCalendarId()))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + activeConnection.getAccessToken())
                    .retrieve()
                    .body(Map.class);
        } catch (ResponseStatusException | RestClientException exception) {
            throw googleSyncFailed(exception);
        }

        List<?> items = response == null || !(response.get("items") instanceof List<?> values)
                ? List.of()
                : values;
        int imported = 0;
        int updated = 0;
        int skipped = 0;
        for (Object item : items) {
            if (!(item instanceof Map<?, ?> googleEvent)) {
                skipped++;
                continue;
            }
            String googleEventId = stringValue(googleEvent.get("id"));
            String status = stringValue(googleEvent.get("status"));
            if (!StringUtils.hasText(googleEventId) || "cancelled".equals(status)) {
                skipped++;
                continue;
            }

            GoogleEventTime googleTime = parseGoogleEventTime(googleEvent);
            CalendarEvent event = calendarEventRepository
                    .findByCoupleIdAndExternalProviderAndExternalEventId(
                            context.coupleId(),
                            CalendarExternalProvider.GOOGLE,
                            googleEventId
                    )
                    .orElse(null);
            if (event == null) {
                event = CalendarEvent.builder()
                        .coupleId(context.coupleId())
                        .ownerId(currentUserId)
                        .createdBy(currentUserId)
                        .title(resolveGoogleSummary(googleEvent))
                        .visibility(CalendarEventVisibility.PERSONAL)
                        .startsAt(googleTime.startsAt())
                        .endsAt(googleTime.endsAt())
                        .allDay(googleTime.allDay())
                        .locationName(stringValue(googleEvent.get("location")))
                        .memo(stringValue(googleEvent.get("description")))
                        .build();
                event.markExternalSync(CalendarExternalProvider.GOOGLE, googleEventId, LocalDateTime.now(ZoneOffset.UTC));
                calendarEventRepository.save(event);
                imported++;
            } else {
                event.update(
                        resolveGoogleSummary(googleEvent),
                        currentUserId,
                        CalendarEventVisibility.PERSONAL,
                        googleTime.startsAt(),
                        googleTime.endsAt(),
                        googleTime.allDay(),
                        stringValue(googleEvent.get("location")),
                        null,
                        null,
                        null,
                        stringValue(googleEvent.get("description")),
                        List.of(),
                        null,
                        null,
                        null
                );
                event.markExternalSync(CalendarExternalProvider.GOOGLE, googleEventId, LocalDateTime.now(ZoneOffset.UTC));
                updated++;
            }
        }

        return GoogleCalendarImportResponse.builder()
                .importedCount(imported)
                .updatedCount(updated)
                .skippedCount(skipped)
                .build();
    }

    private GoogleCalendarOAuthState validateState(String state) {
        if (!StringUtils.hasText(state)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "state is required");
        }
        GoogleCalendarOAuthState oauthState = googleCalendarOAuthStateRepository.findByState(state)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid OAuth state"));
        if (oauthState.isUsed() || oauthState.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "expired OAuth state");
        }
        return oauthState;
    }

    private Map<?, ?> requestToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", resolveRedirectUri());
        form.add("grant_type", "authorization_code");

        return restClient.post()
                .uri(GOOGLE_TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
    }

    private GoogleCalendarConnection ensureFreshAccessToken(GoogleCalendarConnection connection) {
        if (connection.getAccessTokenExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(1))) {
            return connection;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", connection.getRefreshToken());
        form.add("grant_type", "refresh_token");

        Map<?, ?> tokenResponse = restClient.post()
                .uri(GOOGLE_TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
        String accessToken = stringValue(tokenResponse == null ? null : tokenResponse.get("access_token"));
        long expiresInSeconds = longValue(tokenResponse == null ? null : tokenResponse.get("expires_in"), 3600L);
        if (!StringUtils.hasText(accessToken)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google token refresh failed");
        }

        connection.updateTokens(
                accessToken,
                null,
                LocalDateTime.now(ZoneOffset.UTC).plusSeconds(expiresInSeconds),
                connection.getScopes()
        );
        return connection;
    }

    private Map<String, Object> toGoogleEventRequest(CalendarEvent event) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("summary", event.getTitle());
        if (StringUtils.hasText(event.getMemo())) {
            body.put("description", event.getMemo());
        }
        if (StringUtils.hasText(event.getLocationName())) {
            body.put("location", event.getLocationName());
        }
        if (event.isAllDay()) {
            body.put("start", Map.of("date", event.getStartsAt().toLocalDate().toString()));
            body.put("end", Map.of("date", event.getEndsAt().toLocalDate().plusDays(1).toString()));
        } else {
            body.put("start", Map.of(
                    "dateTime", toRfc3339Utc(event.getStartsAt()),
                    "timeZone", "UTC"
            ));
            body.put("end", Map.of(
                    "dateTime", toRfc3339Utc(event.getEndsAt()),
                    "timeZone", "UTC"
            ));
        }
        body.put("extendedProperties", Map.of(
                "private", Map.of("hear2EventId", String.valueOf(event.getId()))
        ));
        return body;
    }

    private GoogleEventTime parseGoogleEventTime(Map<?, ?> googleEvent) {
        Object start = googleEvent.get("start");
        Object end = googleEvent.get("end");
        if (!(start instanceof Map<?, ?> startMap) || !(end instanceof Map<?, ?> endMap)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google event time is invalid");
        }
        String startDate = stringValue(startMap.get("date"));
        String endDate = stringValue(endMap.get("date"));
        if (StringUtils.hasText(startDate)) {
            LocalDate startsAt = LocalDate.parse(startDate);
            LocalDate endsAt = StringUtils.hasText(endDate) ? LocalDate.parse(endDate).minusDays(1) : startsAt;
            return new GoogleEventTime(
                    startsAt.atStartOfDay(),
                    endsAt.atTime(23, 59, 59),
                    true
            );
        }

        String startDateTime = stringValue(startMap.get("dateTime"));
        String endDateTime = stringValue(endMap.get("dateTime"));
        LocalDateTime startsAt = OffsetDateTime.parse(startDateTime).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime endsAt = StringUtils.hasText(endDateTime)
                ? OffsetDateTime.parse(endDateTime).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime()
                : startsAt;
        return new GoogleEventTime(startsAt, endsAt, false);
    }

    private String resolveGoogleSummary(Map<?, ?> googleEvent) {
        String summary = stringValue(googleEvent.get("summary"));
        return StringUtils.hasText(summary) ? summary : "(제목 없음)";
    }

    private CoupleContext resolveCoupleContext(Long currentUserId) {
        validateCurrentUserId(currentUserId);
        CoupleMember member = coupleMemberRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "couple connection not found"));
        return new CoupleContext(member.getCoupleId(), currentUserId);
    }

    private void validateCurrentUserId(Long currentUserId) {
        if (currentUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "authentication is required");
        }
    }

    private void requireGoogleOAuthConfig() {
        if (!isConfigured()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Google OAuth is not configured");
        }
    }

    private boolean isConfigured() {
        return StringUtils.hasText(clientId)
                && StringUtils.hasText(clientSecret)
                && StringUtils.hasText(resolveRedirectUri());
    }

    private String resolveRedirectUri() {
        return googleCalendarProperties.redirectUri();
    }

    private int resolveStateExpirationMinutes() {
        return googleCalendarProperties.oauthStateExpirationMinutes() <= 0
                ? 10
                : googleCalendarProperties.oauthStateExpirationMinutes();
    }

    private String resolveScopes(String scopes) {
        return StringUtils.hasText(scopes) ? scopes : CALENDAR_SCOPES;
    }

    private String toRfc3339Utc(LocalDateTime dateTime) {
        return dateTime.atOffset(ZoneOffset.UTC).format(RFC3339_UTC_FORMATTER);
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private long longValue(Object value, long defaultValue) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null) {
            try {
                return Long.parseLong(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private record CoupleContext(Long coupleId, Long userId) {
    }

    private record GoogleEventTime(LocalDateTime startsAt, LocalDateTime endsAt, boolean allDay) {
    }
}
