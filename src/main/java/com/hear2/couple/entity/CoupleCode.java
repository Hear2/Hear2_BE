package com.hear2.couple.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
        name = "couple_code",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_couple_code_code", columnNames = "code")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CoupleCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "couple_code_id")
    private Long coupleCodeId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "issuer_user_id", nullable = false)
    private Long issuerUserId;

    @Column(name = "used_couple_id")
    private Long usedCoupleId;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public void markUsed(Long coupleId) {
        if (this.usedAt != null) {
            return;
        }
        this.usedCoupleId = coupleId;
        this.usedAt = LocalDateTime.now();
    }
}
