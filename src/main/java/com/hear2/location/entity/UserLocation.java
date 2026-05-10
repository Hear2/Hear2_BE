package com.hear2.location.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_location_couple_user",
                        columnNames = {"coupleId", "userId"}
                )
        },
        indexes = {
                @Index(name = "idx_user_location_couple_updated", columnList = "coupleId, updatedAt")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long coupleId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private Double accuracyMeters;

    @Column(length = 255)
    private String placeName;

    @Column(length = 500)
    private String addressName;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public static UserLocation create(
            Long coupleId,
            Long userId,
            Double latitude,
            Double longitude,
            Double accuracyMeters,
            String placeName,
            String addressName,
            LocalDateTime recordedAt
    ) {
        LocalDateTime now = LocalDateTime.now();
        return UserLocation.builder()
                .coupleId(coupleId)
                .userId(userId)
                .latitude(latitude)
                .longitude(longitude)
                .accuracyMeters(accuracyMeters)
                .placeName(placeName)
                .addressName(addressName)
                .recordedAt(recordedAt == null ? now : recordedAt)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void update(
            Double latitude,
            Double longitude,
            Double accuracyMeters,
            String placeName,
            String addressName,
            LocalDateTime recordedAt
    ) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracyMeters = accuracyMeters;
        this.placeName = placeName;
        this.addressName = addressName;
        this.recordedAt = recordedAt == null ? LocalDateTime.now() : recordedAt;
        this.updatedAt = LocalDateTime.now();
    }
}
