package com.hear2.capsule.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(indexes = {
        @Index(name = "idx_time_capsule_couple_status", columnList = "coupleId, status"),
        @Index(name = "idx_time_capsule_open_at", columnList = "openAt")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class TimeCapsule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long coupleId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TimeCapsuleCoverStyle coverStyle;

    @Column(nullable = false)
    private LocalDateTime openAt;

    @Column(nullable = false)
    private LocalDateTime sealedAt;

    private LocalDateTime openedAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TimeCapsuleStatus status = TimeCapsuleStatus.SEALED;

    @Column(length = 2000)
    private String optionsJson;

    @Getter(AccessLevel.NONE)
    @Column(name = "saved_to_memory", nullable = false)
    @Builder.Default
    private boolean legacySavedToMemory = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = utcNow();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.sealedAt == null) {
            this.sealedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = utcNow();
    }

    public boolean canOpen(LocalDateTime now) {
        return status == TimeCapsuleStatus.SEALED && !openAt.isAfter(now);
    }

    public void open(LocalDateTime openedAt) {
        this.status = TimeCapsuleStatus.OPEN;
        this.openedAt = openedAt;
    }

    private LocalDateTime utcNow() {
        return OffsetDateTime.now(ZoneOffset.UTC).toLocalDateTime();
    }
}
