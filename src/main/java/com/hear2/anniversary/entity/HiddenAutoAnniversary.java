package com.hear2.anniversary.entity;

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
        name = "hidden_auto_anniversary",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_hidden_auto_anniversary_user_type",
                        columnNames = {"coupleId", "userId", "autoKey"}
                )
        },
        indexes = {
                @Index(name = "idx_hidden_auto_anniversary_couple_user", columnList = "coupleId, userId")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class HiddenAutoAnniversary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long coupleId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String autoKey;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
