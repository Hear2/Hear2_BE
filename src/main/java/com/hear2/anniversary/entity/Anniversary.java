package com.hear2.anniversary.entity;

import com.hear2.anniversary.support.AnniversaryDdayType;
import com.hear2.anniversary.support.AnniversaryType;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "anniversary",
        indexes = {
                @Index(name = "idx_anniversary_couple_date", columnList = "coupleId, anniversaryDate"),
                @Index(name = "idx_anniversary_created_by", columnList = "createdBy")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Anniversary {

    private static final int TITLE_MAX_LENGTH = 50;
    private static final int ICON_MAX_LENGTH = 32;
    private static final int COLOR_MAX_LENGTH = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long coupleId;

    @Column(nullable = false)
    private Long createdBy;

    @Column(nullable = false, length = TITLE_MAX_LENGTH)
    private String title;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AnniversaryType type;

    @Column(nullable = false)
    private LocalDate anniversaryDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AnniversaryDdayType ddayType;

    @Column(nullable = false)
    private boolean repeatYearly;

    @Column(nullable = false)
    private boolean shared;

    @Column(nullable = false, length = ICON_MAX_LENGTH)
    private String icon;

    @Column(nullable = false, length = COLOR_MAX_LENGTH)
    private String color;

    @ElementCollection
    @CollectionTable(name = "anniversary_notify_day", joinColumns = @JoinColumn(name = "anniversary_id"))
    @Column(name = "notify_day")
    @Builder.Default
    private List<Integer> notifyDays = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(
            String title,
            AnniversaryType type,
            LocalDate anniversaryDate,
            AnniversaryDdayType ddayType,
            boolean repeatYearly,
            boolean shared,
            String icon,
            String color,
            List<Integer> notifyDays
    ) {
        this.title = title;
        this.type = type;
        this.anniversaryDate = anniversaryDate;
        this.ddayType = ddayType;
        this.repeatYearly = repeatYearly;
        this.shared = shared;
        this.icon = icon;
        this.color = color;
        this.notifyDays.clear();
        this.notifyDays.addAll(notifyDays == null ? List.of() : notifyDays);
    }
}
