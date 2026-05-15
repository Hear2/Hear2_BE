package com.hear2.whatif.entity;

import com.hear2.whatif.support.WhatIfCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "whatif_history", indexes = {
        @Index(name = "idx_whatif_history_couple_created", columnList = "coupleId, createdAt")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class WhatIfHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long coupleId;

    @Column(nullable = false)
    private Long requesterId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WhatIfCategory category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String aiResponseJson;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String usedDataSummaryJson;

    @Column(length = 80)
    private String model;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
