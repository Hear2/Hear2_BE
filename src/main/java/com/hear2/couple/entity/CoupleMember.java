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

import java.time.LocalDateTime;

@Entity
@Table(name = "couple_member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CoupleMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "couple_member_id")
    private Long coupleMemberId;

    @Column(name = "couple_id")
    private Long coupleId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "role")
    private String role;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @PrePersist
    void onJoin() {
        this.joinedAt = LocalDateTime.now();
    }
}
