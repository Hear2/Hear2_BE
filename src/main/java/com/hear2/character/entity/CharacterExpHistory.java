package com.hear2.character.entity;

import com.hear2.character.support.CharacterExpSourceType;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "character_exp_history",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_character_exp_history_source",
                        columnNames = {"source_type", "source_id"}
                )
        },
        indexes = {
                @Index(name = "idx_character_exp_history_couple_date", columnList = "couple_id, earned_date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CharacterExpHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @Column(name = "couple_id", nullable = false)
    private Long coupleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private CharacterExpSourceType sourceType;

    @Column(name = "source_id", nullable = false)
    private String sourceId;

    @Column(name = "exp_amount", nullable = false)
    private Long expAmount;

    @Column(name = "earned_date", nullable = false)
    private LocalDate earnedDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
