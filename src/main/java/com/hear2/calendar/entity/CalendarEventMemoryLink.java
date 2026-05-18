package com.hear2.calendar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
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
        name = "calendar_event_memory_link",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_calendar_event_memory", columnNames = {"eventId", "memoryId"})
        },
        indexes = {
                @Index(name = "idx_calendar_memory_event", columnList = "eventId"),
                @Index(name = "idx_calendar_memory_memory", columnList = "memoryId"),
                @Index(name = "idx_calendar_memory_couple", columnList = "coupleId")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CalendarEventMemoryLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long coupleId;

    @Column(nullable = false)
    private Long eventId;

    @Column(nullable = false)
    private Long memoryId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
