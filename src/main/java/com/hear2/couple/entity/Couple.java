package com.hear2.couple.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "couple")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Couple {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "couple_id")
    private Long coupleId;

    @Column(name = "couple_code")
    private String coupleCode;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void updateStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
}
