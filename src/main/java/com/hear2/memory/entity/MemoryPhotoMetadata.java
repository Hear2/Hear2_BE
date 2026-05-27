package com.hear2.memory.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemoryPhotoMetadata {

    private static final int LOCATION_NAME_MAX_LENGTH = 255;
    private static final int PLACE_NAME_MAX_LENGTH = 255;
    private static final int ADDRESS_NAME_MAX_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memory_id", nullable = false, unique = true)
    private Memory memory;

    private LocalDateTime takenAt;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(length = LOCATION_NAME_MAX_LENGTH)
    private String locationName;

    @Column(length = PLACE_NAME_MAX_LENGTH)
    private String placeName;

    @Column(length = ADDRESS_NAME_MAX_LENGTH)
    private String addressName;

    private MemoryPhotoMetadata(
            LocalDateTime takenAt,
            BigDecimal latitude,
            BigDecimal longitude,
            String locationName,
            String placeName,
            String addressName
    ) {
        this.takenAt = takenAt;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationName = locationName;
        this.placeName = placeName;
        this.addressName = addressName;
    }

    public static MemoryPhotoMetadata create(
            LocalDateTime takenAt,
            BigDecimal latitude,
            BigDecimal longitude,
            String locationName,
            String placeName,
            String addressName
    ) {
        return new MemoryPhotoMetadata(takenAt, latitude, longitude, locationName, placeName, addressName);
    }

    public void assignMemory(Memory memory) {
        this.memory = memory;
    }

    public void updateLocation(
            BigDecimal latitude,
            BigDecimal longitude,
            String locationName,
            String placeName,
            String addressName
    ) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationName = locationName;
        this.placeName = placeName;
        this.addressName = addressName;
    }
}
