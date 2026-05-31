package com.hear2.calendar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "google_calendar_connection",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_google_calendar_connection_user", columnNames = "userId")
        },
        indexes = {
                @Index(name = "idx_google_calendar_connection_user", columnList = "userId")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class GoogleCalendarConnection {

    private static final int TOKEN_MAX_LENGTH = 4096;
    private static final int SCOPE_MAX_LENGTH = 1000;
    private static final int CALENDAR_ID_MAX_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = TOKEN_MAX_LENGTH)
    private String accessToken;

    @Column(nullable = false, length = TOKEN_MAX_LENGTH)
    private String refreshToken;

    @Column(nullable = false)
    private LocalDateTime accessTokenExpiresAt;

    @Column(nullable = false, length = SCOPE_MAX_LENGTH)
    private String scopes;

    @Column(nullable = false, length = CALENDAR_ID_MAX_LENGTH)
    @Builder.Default
    private String calendarId = "primary";

    @Column(nullable = false)
    private LocalDateTime connectedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.connectedAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void updateTokens(
            String accessToken,
            String refreshToken,
            LocalDateTime accessTokenExpiresAt,
            String scopes
    ) {
        this.accessToken = accessToken;
        if (refreshToken != null && !refreshToken.isBlank()) {
            this.refreshToken = refreshToken;
        }
        this.accessTokenExpiresAt = accessTokenExpiresAt;
        this.scopes = scopes;
    }
}
