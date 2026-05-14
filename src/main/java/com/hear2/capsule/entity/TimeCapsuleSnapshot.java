package com.hear2.capsule.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class TimeCapsuleSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long capsuleId;

    @Column(nullable = false)
    private long daysTogether;

    @Column(nullable = false)
    private long messageCount;

    @Column(nullable = false)
    private long photoCount;

    @Column(nullable = false)
    private int characterLevel;

    @Column(nullable = false)
    private LocalDateTime snapshotAt;
}
