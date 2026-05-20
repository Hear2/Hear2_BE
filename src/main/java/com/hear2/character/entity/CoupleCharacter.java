package com.hear2.character.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "couple_character",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_couple_character_couple",
                        columnNames = {"couple_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CoupleCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "character_id")
    private Long characterId;

    @Column(name = "couple_id", nullable = false)
    private Long coupleId;

    @Column(name = "name", nullable = false)
    private String name;

    @Builder.Default
    @Column(name = "exp", nullable = false)
    @ColumnDefault("0")
    private Long exp = 0L;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.exp == null) {
            this.exp = 0L;
        }
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public int getStage() {
        long currentExp = this.exp == null ? 0L : this.exp;
        if (currentExp >= 3500) {
            return 5;
        }
        if (currentExp >= 2000) {
            return 4;
        }
        if (currentExp >= 900) {
            return 3;
        }
        if (currentExp >= 300) {
            return 2;
        }
        return 1;
    }

    public void addExp(long expAmount) {
        if (expAmount <= 0) {
            return;
        }
        if (this.exp == null) {
            this.exp = 0L;
        }
        this.exp += expAmount;
    }
}
