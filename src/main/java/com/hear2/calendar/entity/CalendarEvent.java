package com.hear2.calendar.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "calendar_event",
        indexes = {
                @Index(name = "idx_calendar_event_couple_start", columnList = "coupleId, startsAt"),
                @Index(name = "idx_calendar_event_couple_end", columnList = "coupleId, endsAt"),
                @Index(name = "idx_calendar_event_owner", columnList = "ownerId")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CalendarEvent {

    private static final int TITLE_MAX_LENGTH = 120;
    private static final int LOCATION_MAX_LENGTH = 255;
    private static final int MEMO_MAX_LENGTH = 1000;
    private static final int RECURRENCE_MAX_LENGTH = 255;
    private static final int EXTERNAL_ID_MAX_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long coupleId;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private Long createdBy;

    @Column(nullable = false, length = TITLE_MAX_LENGTH)
    private String title;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CalendarEventVisibility visibility;

    @Column(nullable = false)
    private LocalDateTime startsAt;

    @Column(nullable = false)
    private LocalDateTime endsAt;

    @Column(nullable = false)
    private boolean allDay;

    @Column(length = LOCATION_MAX_LENGTH)
    private String locationName;

    @Column(length = LOCATION_MAX_LENGTH)
    private String addressName;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(length = MEMO_MAX_LENGTH)
    private String memo;

    @ElementCollection
    @CollectionTable(name = "calendar_event_tag", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "tag", length = 80)
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column(length = RECURRENCE_MAX_LENGTH)
    private String recurrenceRule;

    private Integer remindBeforeMinutes;

    private Long linkedChatMessageId;

    @Enumerated(EnumType.STRING)
    private CalendarExternalProvider externalProvider;

    @Column(length = EXTERNAL_ID_MAX_LENGTH)
    private String externalEventId;

    private LocalDateTime externalSyncedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(
            String title,
            Long ownerId,
            CalendarEventVisibility visibility,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            boolean allDay,
            String locationName,
            String addressName,
            BigDecimal latitude,
            BigDecimal longitude,
            String memo,
            List<String> tags,
            String recurrenceRule,
            Integer remindBeforeMinutes,
            Long linkedChatMessageId
    ) {
        this.title = title;
        this.ownerId = ownerId;
        this.visibility = visibility;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.allDay = allDay;
        this.locationName = locationName;
        this.addressName = addressName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.memo = memo;
        replaceTags(tags);
        this.recurrenceRule = recurrenceRule;
        this.remindBeforeMinutes = remindBeforeMinutes;
        this.linkedChatMessageId = linkedChatMessageId;
    }

    public void replaceTags(List<String> tags) {
        this.tags.clear();
        if (tags != null) {
            this.tags.addAll(tags);
        }
    }
}
